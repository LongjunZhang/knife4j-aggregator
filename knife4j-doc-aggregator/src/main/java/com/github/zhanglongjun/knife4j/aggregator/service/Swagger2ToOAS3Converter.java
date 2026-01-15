/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Swagger 2.0 到 OpenAPI 3.0 转换器
 * 
 * 核心功能：
 * 1. 将 Swagger 2.0 格式（/v2/api-docs）转换为 OpenAPI 3.0 格式
 * 2. 提供 JSON 深度规范化功能，确保转换结果幂等
 * 3. 规范化后的 JSON 可用于稳定的 Hash 计算
 */
@Service
public class Swagger2ToOAS3Converter {
    
    private static final Logger log = LoggerFactory.getLogger(Swagger2ToOAS3Converter.class);
    
    /**
     * 用于稳定序列化的 ObjectMapper
     * 配置 ORDER_MAP_ENTRIES_BY_KEYS 确保字段按字母顺序排列
     */
    private final ObjectMapper stableMapper;
    
    public Swagger2ToOAS3Converter() {
        this.stableMapper = new ObjectMapper();
        // 关键配置：确保 Map 按 key 排序
        this.stableMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        // 输出紧凑 JSON（无缩进）
        this.stableMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        
        log.info("Swagger2ToOAS3Converter 已初始化");
    }
    
    /**
     * 将 Swagger 2.0 JSON 转换为 OpenAPI 3.0 JSON
     * 
     * 转换流程：
     * 1. 使用 swagger-parser 解析 Swagger 2.0
     * 2. 自动转换为 OpenAPI 3.0 对象
     * 3. 序列化为 JSON
     * 4. 深度规范化（确保幂等性）
     * 
     * @param swagger2Json Swagger 2.0 格式的 JSON 字符串
     * @return 规范化后的 OpenAPI 3.0 JSON 字符串
     */
    public String convert(String swagger2Json) {
        if (swagger2Json == null || swagger2Json.isEmpty()) {
            log.warn("输入的 Swagger 2.0 JSON 为空");
            return swagger2Json;
        }
        
        try {
            // 使用 swagger-parser 解析并转换
            SwaggerParseResult result = new OpenAPIParser()
                    .readContents(swagger2Json, null, null);
            
            // 检查解析错误
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                log.warn("Swagger 解析警告: {}", result.getMessages());
            }
            
            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null) {
                log.error("Swagger 2.0 解析失败，无法获取 OpenAPI 对象");
                return swagger2Json;
            }
            
            // 序列化为 JSON
            String rawJson = Json.pretty(openAPI);
            
            // 深度规范化确保幂等性
            String normalizedJson = normalizeJson(rawJson);
            
            log.debug("Swagger 2.0 转换为 OpenAPI 3.0 成功，原始长度: {}, 转换后长度: {}", 
                    swagger2Json.length(), normalizedJson.length());
            
            return normalizedJson;
            
        } catch (Exception e) {
            log.error("Swagger 2.0 转换失败: {}", e.getMessage(), e);
            // 转换失败时返回原始内容
            return swagger2Json;
        }
    }
    
    /**
     * 判断内容是否为 Swagger 2.0 格式
     * 
     * @param content JSON 字符串
     * @return true 如果是 Swagger 2.0 格式
     */
    public boolean isSwagger2(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // Swagger 2.0 的特征：包含 "swagger" 字段且版本以 "2." 开头
        return content.contains("\"swagger\"") && 
               (content.contains("\"2.0\"") || content.contains("\"2."));
    }
    
    /**
     * 判断内容是否为 OpenAPI 3.0 格式
     * 
     * @param content JSON 字符串
     * @return true 如果是 OpenAPI 3.0 格式
     */
    public boolean isOpenApi3(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // OpenAPI 3.0 的特征：包含 "openapi" 字段且版本以 "3." 开头
        return content.contains("\"openapi\"") && 
               (content.contains("\"3.0") || content.contains("\"3.1"));
    }
    
    /**
     * 深度规范化 JSON
     * 
     * 规范化流程：
     * 1. 解析 JSON 为对象结构
     * 2. 递归将所有 Map 转换为 TreeMap（按 key 自然排序）
     * 3. 序列化为紧凑 JSON（无缩进、无多余空白）
     * 
     * 这确保了：
     * - 相同内容的 JSON 无论原始字段顺序如何，规范化后都相同
     * - 可以用于稳定的 Hash 计算
     * 
     * @param json 原始 JSON 字符串
     * @return 规范化后的 JSON 字符串
     */
    public String normalizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        try {
            // 解析为通用对象（Map/List/基本类型）
            Object obj = stableMapper.readValue(json, Object.class);
            
            // 深度排序所有 Map
            Object sorted = deepSort(obj);
            
            // 输出为紧凑 JSON
            return stableMapper.writeValueAsString(sorted);
            
        } catch (JsonProcessingException e) {
            log.warn("JSON 规范化失败，返回原始内容: {}", e.getMessage());
            return json;
        }
    }
    
    /**
     * 深度排序对象
     * 
     * 递归处理：
     * - Map: 转换为 TreeMap（按 key 排序），递归处理 value
     * - List: 保持顺序，递归处理每个元素
     * - 其他类型: 直接返回
     * 
     * @param obj 原始对象
     * @return 排序后的对象
     */
    @SuppressWarnings("unchecked")
    private Object deepSort(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sorted.put(entry.getKey(), deepSort(entry.getValue()));
            }
            return sorted;
        }
        
        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            return list.stream()
                    .map(this::deepSort)
                    .collect(Collectors.toList());
        }
        
        // 基本类型直接返回
        return obj;
    }
    
    /**
     * 自动转换：如果是 Swagger 2.0 则转换，否则只规范化
     * 
     * @param content 原始文档内容
     * @return 规范化后的 OpenAPI 3.0 文档
     */
    public String convertIfNeeded(String content) {
        if (isSwagger2(content)) {
            log.info("检测到 Swagger 2.0 格式，执行转换");
            return convert(content);
        }
        
        if (isOpenApi3(content)) {
            log.debug("已是 OpenAPI 3.0 格式，仅执行规范化");
            return normalizeJson(content);
        }
        
        log.warn("未知文档格式，返回原始内容");
        return content;
    }
    
    /**
     * 自动转换（带 contextPath 规范化）：
     * 1. 如果是 Swagger 2.0 则转换为 OpenAPI 3.0
     * 2. 删除 paths 中的 contextPath 前缀
     * 3. 规范化 JSON
     * 
     * @param content 原始文档内容
     * @param contextPath 需要从 paths 中删除的前缀（如 /messageService）
     * @return 规范化后的 OpenAPI 3.0 文档（paths 不带 contextPath 前缀）
     */
    public String convertIfNeeded(String content, String contextPath) {
        String result;
        
        if (isSwagger2(content)) {
            log.info("检测到 Swagger 2.0 格式，执行转换");
            result = convert(content);
        } else if (isOpenApi3(content)) {
            log.debug("已是 OpenAPI 3.0 格式，仅执行规范化");
            result = normalizeJson(content);
        } else {
            log.warn("未知文档格式，返回原始内容");
            return content;
        }
        
        // 删除 paths 中的 contextPath 前缀
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
            result = stripContextPathFromPaths(result, contextPath);
        }
        
        return result;
    }
    
    /**
     * 将 Swagger 2.0 JSON 转换为 OpenAPI 3.0 JSON（带 contextPath 规范化）
     * 
     * @param swagger2Json Swagger 2.0 格式的 JSON 字符串
     * @param contextPath 需要从 paths 中删除的前缀（如 /messageService）
     * @return 规范化后的 OpenAPI 3.0 JSON 字符串（paths 不带 contextPath 前缀）
     */
    public String convert(String swagger2Json, String contextPath) {
        String converted = convert(swagger2Json);
        
        // 删除 paths 中的 contextPath 前缀
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
            converted = stripContextPathFromPaths(converted, contextPath);
        }
        
        return converted;
    }
    
    /**
     * 从 OpenAPI 文档的 paths 中删除 contextPath 前缀
     * 
     * 例如：contextPath = "/messageService"
     * - /messageService/message -> /message
     * - /messageService/message/{id} -> /message/{id}
     * - /other/path -> /other/path（不匹配则保持不变）
     * 
     * @param json OpenAPI 3.0 JSON 字符串
     * @param contextPath 需要删除的前缀（如 /messageService）
     * @return 处理后的 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    public String stripContextPathFromPaths(String json, String contextPath) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            return json;
        }
        
        // 规范化 contextPath：确保以 / 开头，不以 / 结尾
        String normalizedPrefix = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        
        try {
            Map<String, Object> root = stableMapper.readValue(json, Map.class);
            Object pathsObj = root.get("paths");
            
            if (pathsObj == null || !(pathsObj instanceof Map)) {
                return json;
            }
            
            Map<String, Object> paths = (Map<String, Object>) pathsObj;
            Map<String, Object> newPaths = new LinkedHashMap<>();
            int strippedCount = 0;
            
            for (Map.Entry<String, Object> entry : paths.entrySet()) {
                String path = entry.getKey();
                String newPath;
                
                if (path.equals(normalizedPrefix)) {
                    // 路径完全等于 contextPath，替换为 /
                    newPath = "/";
                    strippedCount++;
                } else if (path.startsWith(normalizedPrefix + "/")) {
                    // 路径以 contextPath/ 开头，删除前缀
                    newPath = path.substring(normalizedPrefix.length());
                    strippedCount++;
                } else {
                    // 不匹配，保持不变
                    newPath = path;
                }
                
                newPaths.put(newPath, entry.getValue());
            }
            
            if (strippedCount > 0) {
                log.info("已从 {} 个 paths 中删除 contextPath 前缀: {}", strippedCount, normalizedPrefix);
                root.put("paths", newPaths);
                return stableMapper.writeValueAsString(deepSort(root));
            }
            
            return json;
            
        } catch (JsonProcessingException e) {
            log.warn("删除 paths contextPath 前缀失败: {}", e.getMessage());
            return json;
        }
    }
}

