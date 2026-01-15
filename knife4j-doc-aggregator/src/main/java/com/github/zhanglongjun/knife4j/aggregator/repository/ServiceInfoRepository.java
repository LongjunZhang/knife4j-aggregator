/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.repository;

import com.github.zhanglongjun.knife4j.aggregator.model.ServiceInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 服务元信息 Repository
 */
@Repository
public interface ServiceInfoRepository extends ReactiveMongoRepository<ServiceInfo, String> {
    
    /**
     * 根据服务名查询所有（返回 Flux，处理可能存在的重复数据）
     */
    Flux<ServiceInfo> findAllByServiceName(String serviceName);
    
    /**
     * 根据服务名安全查询（取第一条，避免重复数据导致异常）
     * 使用此方法替代 findByServiceName 以避免 "non unique result" 错误
     */
    default Mono<ServiceInfo> findFirstByServiceName(String serviceName) {
        return findAllByServiceName(serviceName).next();
    }
    
    /**
     * 检查服务是否存在
     */
    Mono<Boolean> existsByServiceName(String serviceName);
    
    /**
     * 查询所有启用的服务（按 order 排序）
     */
    Flux<ServiceInfo> findByEnabledTrueOrderByOrderAsc();
    
    /**
     * 查询所有服务（按 order 排序）
     */
    Flux<ServiceInfo> findAllByOrderByOrderAsc();
    
    /**
     * 根据服务名删除（用于清理重复数据）
     */
    Mono<Long> deleteAllByServiceName(String serviceName);
}
