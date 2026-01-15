/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.cache;

/**
 * API 文档缓存实体
 * 
 * 存储微服务的 OpenAPI 文档内容及其元数据
 */
public class CachedApiDoc {
    
    /** 服务名 */
    private String serviceName;
    
    /** Swagger/OpenAPI JSON 文档内容 */
    private String content;
    
    /** 缓存时间戳（毫秒） */
    private long cachedAt;
    
    /** 硬过期时间（毫秒） */
    private long hardTtl;
    
    /** 服务是否在线 */
    private boolean serviceOnline;
    
    public CachedApiDoc() {
    }
    
    public CachedApiDoc(String serviceName, String content, long hardTtl) {
        this.serviceName = serviceName;
        this.content = content;
        this.cachedAt = System.currentTimeMillis();
        this.hardTtl = hardTtl;
        this.serviceOnline = true;
    }
    
    /**
     * 判断缓存是否已硬过期
     */
    public boolean isHardExpired() {
        return System.currentTimeMillis() - cachedAt > hardTtl;
    }
    
    /**
     * 获取缓存年龄（毫秒）
     */
    public long getAge() {
        return System.currentTimeMillis() - cachedAt;
    }
    
    /**
     * 获取缓存年龄（秒）
     */
    public long getAgeInSeconds() {
        return getAge() / 1000;
    }
    
    /**
     * 更新文档内容并刷新缓存时间
     */
    public void updateContent(String content) {
        this.content = content;
        this.cachedAt = System.currentTimeMillis();
        this.serviceOnline = true;
    }
    
    /**
     * 标记服务下线
     */
    public void markOffline() {
        this.serviceOnline = false;
    }
    
    /**
     * 标记服务上线
     */
    public void markOnline() {
        this.serviceOnline = true;
    }
    
    // Getters and Setters
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getCachedAt() {
        return cachedAt;
    }
    
    public void setCachedAt(long cachedAt) {
        this.cachedAt = cachedAt;
    }
    
    public long getHardTtl() {
        return hardTtl;
    }
    
    public void setHardTtl(long hardTtl) {
        this.hardTtl = hardTtl;
    }
    
    public boolean isServiceOnline() {
        return serviceOnline;
    }
    
    public void setServiceOnline(boolean serviceOnline) {
        this.serviceOnline = serviceOnline;
    }
    
    @Override
    public String toString() {
        return "CachedApiDoc{" +
                "serviceName='" + serviceName + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", cachedAt=" + cachedAt +
                ", ageInSeconds=" + getAgeInSeconds() +
                ", hardExpired=" + isHardExpired() +
                ", serviceOnline=" + serviceOnline +
                '}';
    }
}

