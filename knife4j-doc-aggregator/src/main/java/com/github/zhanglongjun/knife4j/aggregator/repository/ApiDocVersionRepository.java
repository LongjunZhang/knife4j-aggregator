/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.repository;

import com.github.zhanglongjun.knife4j.aggregator.model.ApiDocVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API 文档版本 Repository
 */
@Repository
public interface ApiDocVersionRepository extends ReactiveMongoRepository<ApiDocVersion, String> {
    
    /**
     * 根据服务名获取最新版本
     */
    Mono<ApiDocVersion> findTopByServiceNameOrderByVersionDesc(String serviceName);
    
    /**
     * 根据服务名和版本号查询
     */
    Mono<ApiDocVersion> findByServiceNameAndVersion(String serviceName, String version);
    
    /**
     * 根据服务名和内容哈希查询（用于幂等检测）
     */
    Mono<ApiDocVersion> findByServiceNameAndContentHash(String serviceName, String contentHash);
    
    /**
     * 获取服务的版本列表（按版本号倒序）
     */
    Flux<ApiDocVersion> findByServiceNameOrderByVersionDesc(String serviceName);
    
    /**
     * 获取服务的版本列表（分页）
     */
    Flux<ApiDocVersion> findByServiceNameOrderByVersionDesc(String serviceName, Pageable pageable);
    
    /**
     * 统计服务的版本数量
     */
    Mono<Long> countByServiceName(String serviceName);
    
    /**
     * 删除服务的所有版本
     */
    Mono<Void> deleteByServiceName(String serviceName);
}

