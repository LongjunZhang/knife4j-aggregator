/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 服务元信息实体
 * 
 * 存储服务的基本信息、同步状态、当前版本等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "services")
public class ServiceInfo {
    
    @Id
    private String id;
    
    /**
     * 服务名（唯一）
     */
    @Indexed(unique = true)
    private String serviceName;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 文档拉取路径（默认 /v3/api-docs）
     */
    private String docPath;
    
    /**
     * 排序
     */
    private Integer order;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 最后同步时间
     */
    private Instant lastSyncAt;
    
    /**
     * 最后同步状态：SUCCESS / FAILED / TIMEOUT
     */
    private String lastSyncStatus;
    
    /**
     * 当前版本号（语义化版本，如 "1.0.0"）
     */
    private String currentVersion;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    /**
     * 更新时间
     */
    private Instant updatedAt;
    
    /**
     * 创建新的服务信息
     */
    public static ServiceInfo create(String serviceName, String displayName, String docPath) {
        ServiceInfo info = new ServiceInfo();
        info.setServiceName(serviceName);
        info.setDisplayName(displayName);
        info.setDocPath(docPath != null ? docPath : "/v3/api-docs");
        info.setOrder(1000);
        info.setEnabled(true);
        info.setCurrentVersion("0.0.0");
        info.setCreatedAt(Instant.now());
        info.setUpdatedAt(Instant.now());
        return info;
    }
    
    /**
     * 更新同步状态
     */
    public void updateSyncStatus(String status, String newVersion) {
        this.lastSyncAt = Instant.now();
        this.lastSyncStatus = status;
        if (newVersion != null) {
            this.currentVersion = newVersion;
        }
        this.updatedAt = Instant.now();
    }
}

