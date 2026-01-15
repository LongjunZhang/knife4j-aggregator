/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.repository;

import com.github.zhanglongjun.knife4j.aggregator.model.SyncLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 同步日志 Repository
 */
@Repository
public interface SyncLogRepository extends ReactiveMongoRepository<SyncLog, String> {
    
    /**
     * 根据服务名查询日志（按创建时间倒序）
     */
    Flux<SyncLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);
    
    /**
     * 根据服务名查询日志（分页）
     */
    Flux<SyncLog> findByServiceNameOrderByCreatedAtDesc(String serviceName, Pageable pageable);
    
    /**
     * 根据状态查询日志
     */
    Flux<SyncLog> findByStatus(String status);
    
    /**
     * 根据服务名和状态查询
     */
    Flux<SyncLog> findByServiceNameAndStatus(String serviceName, String status);
    
    /**
     * 获取服务的最新一条日志
     */
    Mono<SyncLog> findTopByServiceNameOrderByCreatedAtDesc(String serviceName);
    
    /**
     * 获取所有日志（按创建时间倒序，分页）
     */
    Flux<SyncLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 统计服务的日志数量
     */
    Mono<Long> countByServiceName(String serviceName);
    
    /**
     * 删除服务的所有日志
     */
    Mono<Void> deleteByServiceName(String serviceName);
}

