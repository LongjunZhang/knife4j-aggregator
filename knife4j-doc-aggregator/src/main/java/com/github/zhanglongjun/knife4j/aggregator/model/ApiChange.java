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
 * 接口变更明细实体
 * 
 * 记录每次版本变更中的接口变化详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_changes")
@CompoundIndexes({
    @CompoundIndex(name = "service_version_idx", def = "{'serviceName': 1, 'version': -1}"),
    @CompoundIndex(name = "change_type_idx", def = "{'changeType': 1}")
})
public class ApiChange {
    
    @Id
    private String id;
    
    /**
     * 服务名
     */
    private String serviceName;
    
    /**
     * 对应的版本号（语义化版本，如 "1.0.0"）
     */
    private String version;
    
    /**
     * 变更类型：ADDED / REMOVED / MODIFIED
     */
    private String changeType;
    
    /**
     * 接口路径
     */
    private String path;
    
    /**
     * HTTP 方法
     */
    private String method;
    
    /**
     * 旧定义（JSON 字符串，可裁剪）
     */
    private String oldValue;
    
    /**
     * 新定义（JSON 字符串）
     */
    private String newValue;
    
    /**
     * 变更描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        ADDED,      // 新增接口
        REMOVED,    // 删除接口
        MODIFIED    // 修改接口
    }
    
    /**
     * 创建新增变更记录
     */
    public static ApiChange added(String serviceName, String version, String path, String method, String newValue) {
        ApiChange change = new ApiChange();
        change.setServiceName(serviceName);
        change.setVersion(version);
        change.setChangeType(ChangeType.ADDED.name());
        change.setPath(path);
        change.setMethod(method.toUpperCase());
        change.setNewValue(newValue);
        change.setDescription("新增接口: " + method.toUpperCase() + " " + path);
        change.setCreatedAt(Instant.now());
        return change;
    }
    
    /**
     * 创建删除变更记录
     */
    public static ApiChange removed(String serviceName, String version, String path, String method, String oldValue) {
        ApiChange change = new ApiChange();
        change.setServiceName(serviceName);
        change.setVersion(version);
        change.setChangeType(ChangeType.REMOVED.name());
        change.setPath(path);
        change.setMethod(method.toUpperCase());
        change.setOldValue(oldValue);
        change.setDescription("删除接口: " + method.toUpperCase() + " " + path);
        change.setCreatedAt(Instant.now());
        return change;
    }
    
    /**
     * 创建修改变更记录
     */
    public static ApiChange modified(String serviceName, String version, String path, String method, 
                                     String oldValue, String newValue) {
        ApiChange change = new ApiChange();
        change.setServiceName(serviceName);
        change.setVersion(version);
        change.setChangeType(ChangeType.MODIFIED.name());
        change.setPath(path);
        change.setMethod(method.toUpperCase());
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setDescription("修改接口: " + method.toUpperCase() + " " + path);
        change.setCreatedAt(Instant.now());
        return change;
    }
}

