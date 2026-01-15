/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.repository;

import com.github.zhanglongjun.knife4j.aggregator.model.ApiChange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 接口变更明细 Repository
 */
@Repository
public interface ApiChangeRepository extends ReactiveMongoRepository<ApiChange, String> {
    
    /**
     * 根据服务名和版本查询变更
     */
    Flux<ApiChange> findByServiceNameAndVersion(String serviceName, String version);
    
    /**
     * 根据服务名查询所有变更（按版本倒序）
     */
    Flux<ApiChange> findByServiceNameOrderByVersionDesc(String serviceName);
    
    /**
     * 根据服务名查询所有变更（分页）
     */
    Flux<ApiChange> findByServiceNameOrderByVersionDesc(String serviceName, Pageable pageable);
    
    /**
     * 根据变更类型查询
     */
    Flux<ApiChange> findByChangeType(String changeType);
    
    /**
     * 根据服务名和变更类型查询
     */
    Flux<ApiChange> findByServiceNameAndChangeType(String serviceName, String changeType);
    
    /**
     * 统计服务的变更数量
     */
    Mono<Long> countByServiceName(String serviceName);
    
    /**
     * 统计某版本的变更数量
     */
    Mono<Long> countByServiceNameAndVersion(String serviceName, String version);
    
    /**
     * 删除服务的所有变更记录
     */
    Mono<Void> deleteByServiceName(String serviceName);
    
    /**
     * 获取最近的变更记录
     */
    Flux<ApiChange> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

