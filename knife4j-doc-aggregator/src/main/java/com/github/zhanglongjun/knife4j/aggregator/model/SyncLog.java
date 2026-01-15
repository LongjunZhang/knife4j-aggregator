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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 同步日志实体
 * 
 * 记录每次文档同步的结果和详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sync_logs")
@CompoundIndexes({
    @CompoundIndex(name = "service_created_idx", def = "{'serviceName': 1, 'createdAt': -1}")
})
public class SyncLog {
    
    @Id
    private String id;
    
    /**
     * 服务名
     */
    private String serviceName;
    
    /**
     * 同步状态：SUCCESS / FAILED / TIMEOUT / NO_CHANGE
     */
    private String status;
    
    /**
     * 同步耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 错误信息（如有）
     */
    private String errorMessage;
    
    /**
     * 产生的新版本号（如有），如 "1.0.0"
     */
    private String newVersion;
    
    /**
     * 变更摘要（如有变更）
     */
    private String changeSummary;
    
    /**
     * 创建时间（设置 7 天 TTL 自动过期）
     */
    @Indexed(expireAfterSeconds = 604800) // 7 天 = 604800 秒
    private Instant createdAt;
    
    /**
     * 同步状态枚举
     */
    public enum Status {
        SUCCESS,    // 同步成功（有变更）
        NO_CHANGE,  // 同步成功（无变更）
        FAILED,     // 同步失败
        TIMEOUT     // 同步超时
    }
    
    /**
     * 创建成功日志（有变更）
     */
    public static SyncLog success(String serviceName, long durationMs, String newVersion, String changeSummary) {
        SyncLog log = new SyncLog();
        log.setServiceName(serviceName);
        log.setStatus(Status.SUCCESS.name());
        log.setDurationMs(durationMs);
        log.setNewVersion(newVersion);
        log.setChangeSummary(changeSummary);
        log.setCreatedAt(Instant.now());
        return log;
    }
    
    /**
     * 创建成功日志（无变更）
     */
    public static SyncLog noChange(String serviceName, long durationMs) {
        SyncLog log = new SyncLog();
        log.setServiceName(serviceName);
        log.setStatus(Status.NO_CHANGE.name());
        log.setDurationMs(durationMs);
        log.setCreatedAt(Instant.now());
        return log;
    }
    
    /**
     * 创建失败日志
     */
    public static SyncLog failed(String serviceName, long durationMs, String errorMessage) {
        SyncLog log = new SyncLog();
        log.setServiceName(serviceName);
        log.setStatus(Status.FAILED.name());
        log.setDurationMs(durationMs);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(Instant.now());
        return log;
    }
    
    /**
     * 创建超时日志
     */
    public static SyncLog timeout(String serviceName, long durationMs) {
        SyncLog log = new SyncLog();
        log.setServiceName(serviceName);
        log.setStatus(Status.TIMEOUT.name());
        log.setDurationMs(durationMs);
        log.setErrorMessage("同步超时");
        log.setCreatedAt(Instant.now());
        return log;
    }
}

