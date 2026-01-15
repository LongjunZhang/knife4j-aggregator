/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * API 文档版本实体
 * 
 * 存储文档的每个版本，支持版本管理和回溯
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_doc_versions")
@CompoundIndexes({
    @CompoundIndex(name = "service_version_unique_idx", def = "{'serviceName': 1, 'version': -1}", unique = true),
    @CompoundIndex(name = "created_at_idx", def = "{'createdAt': -1}")
})
public class ApiDocVersion {
    
    @Id
    private String id;
    
    /**
     * 服务名
     */
    private String serviceName;
    
    /**
     * 版本号（语义化版本，如 "1.0.0"）
     */
    private String version;
    
    /**
     * 完整文档内容（OpenAPI JSON 字符串）
     */
    private String content;
    
    /**
     * 内容哈希（sha256:...）
     */
    private String contentHash;
    
    /**
     * 接口数量
     */
    private Integer apiCount;
    
    /**
     * 变更类型：INITIAL / ADDED / MODIFIED / NO_CHANGE
     */
    private String changeType;
    
    /**
     * 变更摘要
     */
    private String changeSummary;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        INITIAL,    // 首次创建
        ADDED,      // 仅新增接口
        MODIFIED,   // 有修改
        NO_CHANGE   // 无变化（通常不会存储）
    }
    
    /**
     * 创建初始版本 (1.0.0)
     */
    public static ApiDocVersion createInitial(String serviceName, String content, String contentHash, int apiCount) {
        ApiDocVersion version = new ApiDocVersion();
        version.setServiceName(serviceName);
        version.setVersion(SemanticVersion.initial().toString()); // "1.0.0"
        version.setContent(content);
        version.setContentHash(contentHash);
        version.setApiCount(apiCount);
        version.setChangeType(ChangeType.INITIAL.name());
        version.setChangeSummary("初始版本");
        version.setCreatedAt(Instant.now());
        return version;
    }
    
    /**
     * 创建新版本
     * 
     * @param newVersionStr 语义化版本号字符串，如 "1.1.0"
     */
    public static ApiDocVersion createNew(String serviceName, String newVersionStr, String content, 
                                          String contentHash, int apiCount, String changeType, String changeSummary) {
        ApiDocVersion version = new ApiDocVersion();
        version.setServiceName(serviceName);
        version.setVersion(newVersionStr);
        version.setContent(content);
        version.setContentHash(contentHash);
        version.setApiCount(apiCount);
        version.setChangeType(changeType);
        version.setChangeSummary(changeSummary);
        version.setCreatedAt(Instant.now());
        return version;
    }
}

