/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.model.ServiceInfo;
import com.github.zhanglongjun.knife4j.aggregator.repository.ServiceInfoRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据清理服务
 * 
 * 负责在启动时清理重复数据，确保数据一致性
 */
@Service
public class DataCleanupService {
    
    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);
    
    private final ServiceInfoRepository serviceInfoRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    
    public DataCleanupService(ServiceInfoRepository serviceInfoRepository,
                              ReactiveMongoTemplate mongoTemplate) {
        this.serviceInfoRepository = serviceInfoRepository;
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * 应用启动时清理重复的 ServiceInfo 记录
     */
    @PostConstruct
    public void cleanupDuplicatesOnStartup() {
        log.info("开始检查并清理重复的 ServiceInfo 记录...");
        
        cleanupDuplicateServiceInfos()
                .doOnSuccess(count -> {
                    if (count > 0) {
                        log.info("清理完成，共删除 {} 条重复的 ServiceInfo 记录", count);
                    } else {
                        log.info("未发现重复的 ServiceInfo 记录");
                    }
                })
                .doOnError(e -> log.error("清理重复 ServiceInfo 失败", e))
                .subscribe();
    }
    
    /**
     * 清理重复的 ServiceInfo 记录
     * 
     * 逻辑：按 serviceName 分组，保留每组中 createdAt 最早的一条，删除其他重复项
     * 
     * @return 删除的记录数
     */
    public Mono<Integer> cleanupDuplicateServiceInfos() {
        Set<String> seenServiceNames = new HashSet<>();
        AtomicInteger deletedCount = new AtomicInteger(0);
        
        return serviceInfoRepository.findAll()
                .sort((a, b) -> {
                    // 按 createdAt 升序排序，保留最早创建的
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .flatMap(info -> {
                    String serviceName = info.getServiceName();
                    if (seenServiceNames.contains(serviceName)) {
                        // 重复记录，删除
                        log.debug("删除重复的 ServiceInfo: serviceName={}, id={}", serviceName, info.getId());
                        deletedCount.incrementAndGet();
                        return serviceInfoRepository.delete(info).thenReturn(info);
                    } else {
                        // 首次看到，保留
                        seenServiceNames.add(serviceName);
                        return Mono.just(info);
                    }
                })
                .then(Mono.fromCallable(deletedCount::get));
    }
    
    /**
     * 手动触发清理重复数据
     */
    public Mono<CleanupResult> cleanupAllDuplicates() {
        return cleanupDuplicateServiceInfos()
                .map(count -> new CleanupResult(count, "ServiceInfo 重复数据清理完成"));
    }
    
    /**
     * 清理结果
     */
    public static class CleanupResult {
        private final int deletedCount;
        private final String message;
        
        public CleanupResult(int deletedCount, String message) {
            this.deletedCount = deletedCount;
            this.message = message;
        }
        
        public int getDeletedCount() {
            return deletedCount;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

