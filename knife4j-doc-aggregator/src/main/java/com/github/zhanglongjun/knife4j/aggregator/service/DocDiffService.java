/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiChange;
import com.github.zhanglongjun.knife4j.aggregator.model.SemanticVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.SemanticVersion.ChangeLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 文档对比服务
 * 
 * 负责对比两个 OpenAPI 文档，生成变更列表
 */
@Service
public class DocDiffService {
    
    private static final Logger log = LoggerFactory.getLogger(DocDiffService.class);
    
    private final ObjectMapper objectMapper;
    
    public DocDiffService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 对比结果
     */
    public static class DiffResult {
        private final List<ApiChange> changes;
        private final int addedCount;
        private final int removedCount;
        private final int modifiedCount;
        private final String summary;
        private final ChangeLevel changeLevel;
        private final int tagsAdded;
        private final int tagsRemoved;
        private final int pathsAdded;
        private final int pathsRemoved;
        
        public DiffResult(List<ApiChange> changes, int tagsAdded, int tagsRemoved, 
                         int pathsAdded, int pathsRemoved) {
            this.changes = changes;
            this.tagsAdded = tagsAdded;
            this.tagsRemoved = tagsRemoved;
            this.pathsAdded = pathsAdded;
            this.pathsRemoved = pathsRemoved;
            this.addedCount = (int) changes.stream()
                    .filter(c -> ApiChange.ChangeType.ADDED.name().equals(c.getChangeType()))
                    .count();
            this.removedCount = (int) changes.stream()
                    .filter(c -> ApiChange.ChangeType.REMOVED.name().equals(c.getChangeType()))
                    .count();
            this.modifiedCount = (int) changes.stream()
                    .filter(c -> ApiChange.ChangeType.MODIFIED.name().equals(c.getChangeType()))
                    .count();
            this.changeLevel = calculateChangeLevel();
            this.summary = generateSummary();
        }
        
        /**
         * 根据变更类型计算版本升级级别
         * 优先级: MAJOR > MINOR > PATCH
         */
        private ChangeLevel calculateChangeLevel() {
            // 无变更
            if (changes.isEmpty() && tagsAdded == 0 && tagsRemoved == 0) {
                return ChangeLevel.NONE;
            }
            
            // Tag 有增删 -> MAJOR
            if (tagsAdded > 0 || tagsRemoved > 0) {
                return ChangeLevel.MAJOR;
            }
            
            // Path 有增删 -> MINOR
            if (pathsAdded > 0 || pathsRemoved > 0) {
                return ChangeLevel.MINOR;
            }
            
            // 仅接口内容修改 -> PATCH
            if (modifiedCount > 0) {
                return ChangeLevel.PATCH;
            }
            
            // 默认返回 PATCH（理论上不会走到这里）
            return ChangeLevel.PATCH;
        }
        
        private String generateSummary() {
            if (changes.isEmpty() && tagsAdded == 0 && tagsRemoved == 0) {
                return "无变更";
            }
            
            StringBuilder sb = new StringBuilder();
            if (tagsAdded > 0) {
                sb.append("新增 ").append(tagsAdded).append(" 个模块");
            }
            if (tagsRemoved > 0) {
                if (sb.length() > 0) sb.append("，");
                sb.append("删除 ").append(tagsRemoved).append(" 个模块");
            }
            if (addedCount > 0) {
                if (sb.length() > 0) sb.append("，");
                sb.append("新增 ").append(addedCount).append(" 个接口");
            }
            if (removedCount > 0) {
                if (sb.length() > 0) sb.append("，");
                sb.append("删除 ").append(removedCount).append(" 个接口");
            }
            if (modifiedCount > 0) {
                if (sb.length() > 0) sb.append("，");
                sb.append("修改 ").append(modifiedCount).append(" 个接口");
            }
            return sb.toString();
        }
        
        public List<ApiChange> getChanges() { return changes; }
        public int getAddedCount() { return addedCount; }
        public int getRemovedCount() { return removedCount; }
        public int getModifiedCount() { return modifiedCount; }
        public String getSummary() { return summary; }
        public boolean hasChanges() { return changeLevel != ChangeLevel.NONE; }
        public ChangeLevel getChangeLevel() { return changeLevel; }
        public int getTagsAdded() { return tagsAdded; }
        public int getTagsRemoved() { return tagsRemoved; }
        public int getPathsAdded() { return pathsAdded; }
        public int getPathsRemoved() { return pathsRemoved; }
        
        public String getChangeType() {
            if (changeLevel == ChangeLevel.NONE) {
                return "NO_CHANGE";
            }
            if (removedCount > 0 || modifiedCount > 0 || tagsRemoved > 0) {
                return "MODIFIED";
            }
            return "ADDED";
        }
    }
    
    /**
     * 对比两个文档内容
     * 
     * @param oldContent 旧文档内容
     * @param newContent 新文档内容
     * @param serviceName 服务名
     * @param newVersion 新版本号（语义化版本字符串）
     * @return 对比结果
     */
    public DiffResult diff(String oldContent, String newContent, String serviceName, String newVersion) {
        List<ApiChange> changes = new ArrayList<>();
        int tagsAdded = 0;
        int tagsRemoved = 0;
        int pathsAdded = 0;
        int pathsRemoved = 0;
        
        try {
            JsonNode oldDoc = objectMapper.readTree(oldContent);
            JsonNode newDoc = objectMapper.readTree(newContent);
            
            // ========== 对比 Tags（模块） ==========
            Set<String> oldTagNames = extractTagNames(oldDoc.get("tags"));
            Set<String> newTagNames = extractTagNames(newDoc.get("tags"));
            
            // 新增的 Tags
            for (String tag : newTagNames) {
                if (!oldTagNames.contains(tag)) {
                    tagsAdded++;
                }
            }
            
            // 删除的 Tags
            for (String tag : oldTagNames) {
                if (!newTagNames.contains(tag)) {
                    tagsRemoved++;
                }
            }
            
            // ========== 对比 Paths（接口） ==========
            JsonNode oldPaths = oldDoc.get("paths");
            JsonNode newPaths = newDoc.get("paths");
            
            if (oldPaths == null) oldPaths = objectMapper.createObjectNode();
            if (newPaths == null) newPaths = objectMapper.createObjectNode();
            
            Set<String> oldPathSet = new HashSet<>();
            Set<String> newPathSet = new HashSet<>();
            
            oldPaths.fieldNames().forEachRemaining(oldPathSet::add);
            newPaths.fieldNames().forEachRemaining(newPathSet::add);
            
            // 检测新增的路径
            for (String path : newPathSet) {
                if (!oldPathSet.contains(path)) {
                    pathsAdded++;
                    // 整个路径是新增的
                    JsonNode pathNode = newPaths.get(path);
                    Iterator<String> methods = pathNode.fieldNames();
                    while (methods.hasNext()) {
                        String method = methods.next();
                        if (isHttpMethod(method)) {
                            String newValue = truncateJson(pathNode.get(method));
                            changes.add(ApiChange.added(serviceName, newVersion, path, method, newValue));
                        }
                    }
                }
            }
            
            // 检测删除的路径
            for (String path : oldPathSet) {
                if (!newPathSet.contains(path)) {
                    pathsRemoved++;
                    // 整个路径被删除
                    JsonNode pathNode = oldPaths.get(path);
                    Iterator<String> methods = pathNode.fieldNames();
                    while (methods.hasNext()) {
                        String method = methods.next();
                        if (isHttpMethod(method)) {
                            String oldValue = truncateJson(pathNode.get(method));
                            changes.add(ApiChange.removed(serviceName, newVersion, path, method, oldValue));
                        }
                    }
                }
            }
            
            // 检测共有路径中的方法变更
            Set<String> commonPaths = new HashSet<>(oldPathSet);
            commonPaths.retainAll(newPathSet);
            
            for (String path : commonPaths) {
                compareMethods(path, oldPaths.get(path), newPaths.get(path), 
                              serviceName, newVersion, changes);
            }
            
            log.info("服务 {} 文档对比完成：Tags +{} -{}, Paths +{} -{}, 接口变更 {} 个", 
                    serviceName, tagsAdded, tagsRemoved, pathsAdded, pathsRemoved, changes.size());
            
        } catch (JsonProcessingException e) {
            log.error("解析文档失败，无法进行对比", e);
        }
        
        return new DiffResult(changes, tagsAdded, tagsRemoved, pathsAdded, pathsRemoved);
    }
    
    /**
     * 从 tags 节点提取 tag 名称集合
     */
    private Set<String> extractTagNames(JsonNode tagsNode) {
        Set<String> tagNames = new HashSet<>();
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                JsonNode nameNode = tag.get("name");
                if (nameNode != null && nameNode.isTextual()) {
                    tagNames.add(nameNode.asText());
                }
            }
        }
        return tagNames;
    }
    
    /**
     * 对比同一路径下的方法变更
     */
    private void compareMethods(String path, JsonNode oldPathNode, JsonNode newPathNode,
                                String serviceName, String newVersion, List<ApiChange> changes) {
        Set<String> oldMethods = new HashSet<>();
        Set<String> newMethods = new HashSet<>();
        
        oldPathNode.fieldNames().forEachRemaining(m -> {
            if (isHttpMethod(m)) oldMethods.add(m);
        });
        newPathNode.fieldNames().forEachRemaining(m -> {
            if (isHttpMethod(m)) newMethods.add(m);
        });
        
        // 新增的方法
        for (String method : newMethods) {
            if (!oldMethods.contains(method)) {
                String newValue = truncateJson(newPathNode.get(method));
                changes.add(ApiChange.added(serviceName, newVersion, path, method, newValue));
            }
        }
        
        // 删除的方法
        for (String method : oldMethods) {
            if (!newMethods.contains(method)) {
                String oldValue = truncateJson(oldPathNode.get(method));
                changes.add(ApiChange.removed(serviceName, newVersion, path, method, oldValue));
            }
        }
        
        // 修改的方法
        Set<String> commonMethods = new HashSet<>(oldMethods);
        commonMethods.retainAll(newMethods);
        
        for (String method : commonMethods) {
            JsonNode oldMethod = oldPathNode.get(method);
            JsonNode newMethod = newPathNode.get(method);
            
            if (!oldMethod.equals(newMethod)) {
                String oldValue = truncateJson(oldMethod);
                String newValue = truncateJson(newMethod);
                changes.add(ApiChange.modified(serviceName, newVersion, path, method, oldValue, newValue));
            }
        }
    }
    
    /**
     * 判断是否为 HTTP 方法
     */
    private boolean isHttpMethod(String method) {
        String upper = method.toUpperCase();
        return "GET".equals(upper) || "POST".equals(upper) || "PUT".equals(upper) 
                || "DELETE".equals(upper) || "PATCH".equals(upper) || "HEAD".equals(upper) 
                || "OPTIONS".equals(upper) || "TRACE".equals(upper);
    }
    
    /**
     * 裁剪 JSON 内容（避免存储过大的数据）
     */
    private String truncateJson(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(node);
            // 如果超过 10KB，进行裁剪
            if (json.length() > 10240) {
                return json.substring(0, 10240) + "...（已裁剪）";
            }
            return json;
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }
    
    /**
     * 对比两个版本的文档（用于查询对比，不用于存储）
     */
    public DiffResult diffVersions(String oldContent, String newContent, String serviceName) {
        return diff(oldContent, newContent, serviceName, "0.0.0");
    }
    
    /**
     * 快速判断变更级别（不生成详细变更列表）
     * 用于确定新版本号
     */
    public ChangeLevel determineChangeLevel(String oldContent, String newContent) {
        try {
            JsonNode oldDoc = objectMapper.readTree(oldContent);
            JsonNode newDoc = objectMapper.readTree(newContent);
            
            // 1. 检查 Tags 变更 -> MAJOR
            Set<String> oldTagNames = extractTagNames(oldDoc.get("tags"));
            Set<String> newTagNames = extractTagNames(newDoc.get("tags"));
            
            if (!oldTagNames.equals(newTagNames)) {
                return ChangeLevel.MAJOR;
            }
            
            // 2. 检查 Paths 增删 -> MINOR
            JsonNode oldPaths = oldDoc.get("paths");
            JsonNode newPaths = newDoc.get("paths");
            
            Set<String> oldPathSet = new HashSet<>();
            Set<String> newPathSet = new HashSet<>();
            
            if (oldPaths != null) oldPaths.fieldNames().forEachRemaining(oldPathSet::add);
            if (newPaths != null) newPaths.fieldNames().forEachRemaining(newPathSet::add);
            
            if (!oldPathSet.equals(newPathSet)) {
                return ChangeLevel.MINOR;
            }
            
            // 3. 检查接口内容修改 -> PATCH
            for (String path : oldPathSet) {
                JsonNode oldPathNode = oldPaths.get(path);
                JsonNode newPathNode = newPaths.get(path);
                if (!oldPathNode.equals(newPathNode)) {
                    return ChangeLevel.PATCH;
                }
            }
            
            return ChangeLevel.NONE;
            
        } catch (JsonProcessingException e) {
            log.error("解析文档失败，无法判断变更级别", e);
            return ChangeLevel.PATCH; // 默认返回 PATCH
        }
    }
}

