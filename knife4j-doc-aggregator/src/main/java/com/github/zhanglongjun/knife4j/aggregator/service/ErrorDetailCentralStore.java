package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.config.ErrorCollectorProxyProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 错误详情集中存储服务
 * 
 * 存储从业务服务收集的错误详情，供 AI 分析时使用。
 * 基于 ConcurrentHashMap 实现，支持 TTL 和定时清理。
 */
@Slf4j
@Service
public class ErrorDetailCentralStore {

    private final Map<String, ErrorDetailEntry> store = new ConcurrentHashMap<>();
    private final ErrorCollectorProxyProperties properties;

    public ErrorDetailCentralStore(ErrorCollectorProxyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("ErrorDetailCentralStore initialized: ttl={}h, maxEntries={}",
                properties.getCentralStoreTtlHours(), properties.getMaxCentralStoredErrors());
    }

    @PreDestroy
    public void destroy() {
        log.info("ErrorDetailCentralStore shutting down, clearing {} entries", store.size());
        store.clear();
    }

    /**
     * 存储错误详情
     *
     * @param errorId 错误 ID
     * @param detail  错误详情（Map 格式）
     * @return 存储成功返回 true
     */
    public boolean store(String errorId, Map<String, Object> detail) {
        if (errorId == null || detail == null) {
            return false;
        }

        // 检查容量
        if (store.size() >= properties.getMaxCentralStoredErrors()) {
            log.warn("ErrorDetailCentralStore reached max capacity ({}), cleaning up first",
                    properties.getMaxCentralStoredErrors());
            cleanup();

            // 再次检查
            if (store.size() >= properties.getMaxCentralStoredErrors()) {
                log.error("ErrorDetailCentralStore still at max capacity after cleanup, rejecting new error");
                return false;
            }
        }

        Instant expiresAt = Instant.now()
                .plus(properties.getCentralStoreTtlHours(), ChronoUnit.HOURS);
        
        store.put(errorId, new ErrorDetailEntry(detail, expiresAt));
        log.debug("Stored error detail: errorId={}, expiresAt={}, storeSize={}",
                errorId, expiresAt, store.size());
        return true;
    }

    /**
     * 根据 errorId 获取错误详情
     *
     * @param errorId 错误 ID
     * @return 错误详情，如果不存在或已过期返回 empty
     */
    public Optional<Map<String, Object>> get(String errorId) {
        if (errorId == null) {
            return Optional.empty();
        }

        ErrorDetailEntry entry = store.get(errorId);
        if (entry == null) {
            log.debug("Error detail not found: errorId={}", errorId);
            return Optional.empty();
        }

        // 检查是否过期
        if (Instant.now().isAfter(entry.getExpiresAt())) {
            store.remove(errorId);
            log.debug("Error detail expired and removed: errorId={}", errorId);
            return Optional.empty();
        }

        return Optional.of(entry.getDetail());
    }

    /**
     * 删除错误详情
     *
     * @param errorId 错误 ID
     * @return 如果存在并删除返回 true
     */
    public boolean remove(String errorId) {
        return store.remove(errorId) != null;
    }

    /**
     * 获取当前存储数量
     */
    public int size() {
        return store.size();
    }

    /**
     * 清理过期的错误详情
     *
     * @return 清理的数量
     */
    @Scheduled(fixedRateString = "${knife4j.error-collector-proxy.cleanup-interval-minutes:5}000")
    public int cleanup() {
        Instant now = Instant.now();
        AtomicInteger count = new AtomicInteger(0);

        store.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().getExpiresAt())) {
                count.incrementAndGet();
                return true;
            }
            return false;
        });

        if (count.get() > 0) {
            log.info("Cleaned up {} expired error details, remaining: {}", count.get(), store.size());
        }

        return count.get();
    }

    /**
     * 获取存储状态
     */
    public StoreStatus getStatus() {
        return new StoreStatus(
                store.size(),
                properties.getMaxCentralStoredErrors(),
                properties.getCentralStoreTtlHours()
        );
    }

    /**
     * 错误详情条目
     */
    @Data
    @AllArgsConstructor
    public static class ErrorDetailEntry {
        private Map<String, Object> detail;
        private Instant expiresAt;
    }

    /**
     * 存储状态
     */
    @Data
    @AllArgsConstructor
    public static class StoreStatus {
        private int currentSize;
        private int maxCapacity;
        private int ttlHours;
    }
}
