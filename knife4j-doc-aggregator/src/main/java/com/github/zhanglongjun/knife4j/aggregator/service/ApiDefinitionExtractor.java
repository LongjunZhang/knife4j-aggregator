/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.aggregator.dto.ApiDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * API 定义提取器
 * 
 * 从 OpenAPI 文档中提取指定接口的最小定义，用于 AI 生成参数和解释错误。
 */
@Slf4j
@Service
public class ApiDefinitionExtractor {
    
    private final DocVersionService docVersionService;
    private final ObjectMapper objectMapper;
    
    public ApiDefinitionExtractor(DocVersionService docVersionService) {
        this.docVersionService = docVersionService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 提取 API 定义
     * 
     * @param serviceName 服务名
     * @param docVersion 文档版本
     * @param path 接口路径
     * @param method HTTP 方法
     * @return API 定义
     */
    public Mono<ApiDefinition> extract(String serviceName, String docVersion, 
                                        String path, String method) {
        return docVersionService.getVersion(serviceName, docVersion)
            .map(doc -> {
                try {
                    return parseApiDefinition(doc.getContent(), path, method, serviceName, docVersion);
                } catch (Exception e) {
                    log.error("解析 API 定义失败: serviceName={}, path={}, method={}", 
                        serviceName, path, method, e);
                    return createEmptyDefinition(serviceName, docVersion, path, method);
                }
            })
            .defaultIfEmpty(createEmptyDefinition(serviceName, docVersion, path, method));
    }
    
    /**
     * 解析 API 定义
     */
    private ApiDefinition parseApiDefinition(String content, String path, String method,
                                              String serviceName, String docVersion) 
            throws JsonProcessingException {
        
        JsonNode root = objectMapper.readTree(content);
        JsonNode paths = root.get("paths");
        
        if (paths == null) {
            log.warn("OpenAPI 文档中没有 paths 定义");
            return createEmptyDefinition(serviceName, docVersion, path, method);
        }
        
        // 尝试直接匹配路径，或进行模板匹配
        JsonNode pathNode = findPathNode(paths, path);
        if (pathNode == null) {
            log.warn("未找到路径定义: {}", path);
            return createEmptyDefinition(serviceName, docVersion, path, method);
        }
        
        // 获取方法定义
        String methodLower = method.toLowerCase();
        JsonNode operationNode = pathNode.get(methodLower);
        if (operationNode == null) {
            log.warn("未找到方法定义: {} {}", method, path);
            return createEmptyDefinition(serviceName, docVersion, path, method);
        }
        
        // 构建 ApiDefinition
        return buildApiDefinition(root, operationNode, pathNode, 
            serviceName, docVersion, path, method);
    }
    
    /**
     * 查找路径节点，支持路径模板匹配
     */
    private JsonNode findPathNode(JsonNode paths, String targetPath) {
        // 先尝试直接匹配
        if (paths.has(targetPath)) {
            return paths.get(targetPath);
        }
        
        // 尝试模板匹配（处理 /user/{id} 这种情况）
        Iterator<String> fieldNames = paths.fieldNames();
        while (fieldNames.hasNext()) {
            String pathTemplate = fieldNames.next();
            if (pathMatches(pathTemplate, targetPath)) {
                return paths.get(pathTemplate);
            }
        }
        
        return null;
    }
    
    /**
     * 检查路径是否匹配（支持路径变量）
     */
    private boolean pathMatches(String template, String actual) {
        // 将模板中的 {xxx} 替换为正则表达式
        String regex = template.replaceAll("\\{[^}]+\\}", "[^/]+");
        return actual.matches(regex);
    }
    
    /**
     * 构建 API 定义
     */
    private ApiDefinition buildApiDefinition(JsonNode root, JsonNode operationNode, 
                                              JsonNode pathNode, String serviceName, 
                                              String docVersion, String path, String method) {
        
        ApiDefinition.ApiDefinitionBuilder builder = ApiDefinition.builder()
            .serviceName(serviceName)
            .docVersion(docVersion)
            .path(path)
            .method(method.toUpperCase());
        
        // 提取摘要和描述
        if (operationNode.has("summary")) {
            builder.summary(operationNode.get("summary").asText());
        }
        if (operationNode.has("description")) {
            builder.description(operationNode.get("description").asText());
        }
        
        // 提取参数
        List<ApiDefinition.ParameterDef> parameters = new ArrayList<>();
        
        // 路径级别的参数
        if (pathNode.has("parameters")) {
            extractParameters(root, pathNode.get("parameters"), parameters);
        }
        
        // 操作级别的参数
        if (operationNode.has("parameters")) {
            extractParameters(root, operationNode.get("parameters"), parameters);
        }
        
        builder.parameters(parameters);
        
        // 提取请求体 Schema 和 Content-Type
        if (operationNode.has("requestBody")) {
            JsonNode requestBody = operationNode.get("requestBody");
            extractRequestBody(root, requestBody, builder);
        }
        
        // 提取响应定义
        if (operationNode.has("responses")) {
            Map<String, Object> responses = extractResponses(root, operationNode.get("responses"));
            builder.responses(responses);
        }
        
        return builder.build();
    }
    
    /**
     * 提取参数列表
     */
    private void extractParameters(JsonNode root, JsonNode parametersNode, 
                                    List<ApiDefinition.ParameterDef> parameters) {
        if (parametersNode == null || !parametersNode.isArray()) {
            return;
        }
        
        for (JsonNode paramNode : parametersNode) {
            // 处理 $ref 引用
            JsonNode resolvedParam = resolveRef(root, paramNode);
            if (resolvedParam == null) continue;
            
            ApiDefinition.ParameterDef param = ApiDefinition.ParameterDef.builder()
                .in(getTextValue(resolvedParam, "in", "query"))
                .name(getTextValue(resolvedParam, "name", ""))
                .required(getBooleanValue(resolvedParam, "required", false))
                .description(getTextValue(resolvedParam, "description", null))
                .build();
            
            // 提取 schema
            if (resolvedParam.has("schema")) {
                Map<String, Object> schema = resolveSchema(root, resolvedParam.get("schema"));
                param.setSchema(schema);
            }
            
            parameters.add(param);
        }
    }
    
    /**
     * 提取请求体
     */
    private void extractRequestBody(JsonNode root, JsonNode requestBody,
                                     ApiDefinition.ApiDefinitionBuilder builder) {
        // 处理 $ref 引用
        JsonNode resolvedBody = resolveRef(root, requestBody);
        if (resolvedBody == null) return;
        
        if (resolvedBody.has("content")) {
            JsonNode content = resolvedBody.get("content");
            
            // 优先使用 application/json
            String[] contentTypes = {"application/json", "application/x-www-form-urlencoded", 
                                      "multipart/form-data", "text/plain"};
            
            for (String contentType : contentTypes) {
                if (content.has(contentType)) {
                    builder.contentType(contentType);
                    
                    JsonNode mediaType = content.get(contentType);
                    if (mediaType.has("schema")) {
                        Map<String, Object> schema = resolveSchema(root, mediaType.get("schema"));
                        builder.requestBodySchema(schema);
                    }
                    break;
                }
            }
            
            // 如果没有匹配到已知类型，取第一个
            if (builder.build().getContentType() == null) {
                Iterator<String> fieldNames = content.fieldNames();
                if (fieldNames.hasNext()) {
                    String firstContentType = fieldNames.next();
                    builder.contentType(firstContentType);
                    
                    JsonNode mediaType = content.get(firstContentType);
                    if (mediaType.has("schema")) {
                        Map<String, Object> schema = resolveSchema(root, mediaType.get("schema"));
                        builder.requestBodySchema(schema);
                    }
                }
            }
        }
    }
    
    /**
     * 提取响应定义（简化版）
     */
    private Map<String, Object> extractResponses(JsonNode root, JsonNode responsesNode) {
        Map<String, Object> responses = new LinkedHashMap<>();
        
        Iterator<String> statusCodes = responsesNode.fieldNames();
        while (statusCodes.hasNext()) {
            String statusCode = statusCodes.next();
            JsonNode responseNode = responsesNode.get(statusCode);
            
            // 处理 $ref 引用
            JsonNode resolvedResponse = resolveRef(root, responseNode);
            if (resolvedResponse == null) continue;
            
            Map<String, Object> responseInfo = new LinkedHashMap<>();
            
            if (resolvedResponse.has("description")) {
                responseInfo.put("description", resolvedResponse.get("description").asText());
            }
            
            // 简化：只提取描述，不展开完整 schema（避免响应过大）
            responses.put(statusCode, responseInfo);
        }
        
        return responses;
    }
    
    /**
     * 解析 $ref 引用
     */
    private JsonNode resolveRef(JsonNode root, JsonNode node) {
        if (node == null) return null;
        
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            return resolveRefPath(root, ref);
        }
        
        return node;
    }
    
    /**
     * 根据 $ref 路径解析节点
     */
    private JsonNode resolveRefPath(JsonNode root, String ref) {
        if (ref == null || !ref.startsWith("#/")) {
            return null;
        }
        
        String[] parts = ref.substring(2).split("/");
        JsonNode current = root;
        
        for (String part : parts) {
            if (current == null) return null;
            // 处理 URL 编码的字符
            part = part.replace("~1", "/").replace("~0", "~");
            current = current.get(part);
        }
        
        return current;
    }
    
    /**
     * 解析 Schema（展开 $ref，限制深度避免循环引用）
     */
    private Map<String, Object> resolveSchema(JsonNode root, JsonNode schemaNode) {
        return resolveSchemaWithDepth(root, schemaNode, 0, 5);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSchemaWithDepth(JsonNode root, JsonNode schemaNode, 
                                                        int depth, int maxDepth) {
        if (schemaNode == null || depth > maxDepth) {
            return new LinkedHashMap<>();
        }
        
        // 处理 $ref 引用
        if (schemaNode.has("$ref")) {
            String ref = schemaNode.get("$ref").asText();
            JsonNode resolved = resolveRefPath(root, ref);
            if (resolved != null) {
                return resolveSchemaWithDepth(root, resolved, depth + 1, maxDepth);
            }
            return new LinkedHashMap<>();
        }
        
        try {
            Map<String, Object> schema = objectMapper.treeToValue(schemaNode, Map.class);
            
            // 递归处理 properties
            if (schema.containsKey("properties") && schema.get("properties") instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                Map<String, Object> resolvedProps = new LinkedHashMap<>();
                
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> propValue = (Map<String, Object>) entry.getValue();
                        if (propValue.containsKey("$ref")) {
                            String ref = (String) propValue.get("$ref");
                            JsonNode resolved = resolveRefPath(root, ref);
                            if (resolved != null) {
                                resolvedProps.put(entry.getKey(), 
                                    resolveSchemaWithDepth(root, resolved, depth + 1, maxDepth));
                                continue;
                            }
                        }
                    }
                    resolvedProps.put(entry.getKey(), entry.getValue());
                }
                
                schema.put("properties", resolvedProps);
            }
            
            // 递归处理 items（数组类型）
            if (schema.containsKey("items") && schema.get("items") instanceof Map) {
                Map<String, Object> items = (Map<String, Object>) schema.get("items");
                if (items.containsKey("$ref")) {
                    String ref = (String) items.get("$ref");
                    JsonNode resolved = resolveRefPath(root, ref);
                    if (resolved != null) {
                        schema.put("items", 
                            resolveSchemaWithDepth(root, resolved, depth + 1, maxDepth));
                    }
                }
            }
            
            return schema;
            
        } catch (Exception e) {
            log.warn("解析 Schema 失败", e);
            return new LinkedHashMap<>();
        }
    }
    
    /**
     * 获取文本值
     */
    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field)) {
            return defaultValue;
        }
        return node.get(field).asText(defaultValue);
    }
    
    /**
     * 获取布尔值
     */
    private boolean getBooleanValue(JsonNode node, String field, boolean defaultValue) {
        if (node == null || !node.has(field)) {
            return defaultValue;
        }
        return node.get(field).asBoolean(defaultValue);
    }
    
    /**
     * 创建空的 API 定义
     */
    private ApiDefinition createEmptyDefinition(String serviceName, String docVersion,
                                                 String path, String method) {
        return ApiDefinition.builder()
            .serviceName(serviceName)
            .docVersion(docVersion)
            .path(path)
            .method(method.toUpperCase())
            .parameters(new ArrayList<>())
            .build();
    }
}

