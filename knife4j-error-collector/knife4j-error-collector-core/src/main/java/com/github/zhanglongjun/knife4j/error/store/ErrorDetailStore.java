package com.github.zhanglongjun.knife4j.error.store;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.model.ErrorDetail;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 错误详情内存存储
 * 使用 ConcurrentHashMap 存储，定时清理过期数据
 */
@Slf4j
public class ErrorDetailStore {

    private final Map<String, ErrorDetail> store = new ConcurrentHashMap<>();
    private final ErrorCollectorProperties properties;
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public ErrorDetailStore(ErrorCollectorProperties properties) {
        this.properties = properties;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "error-detail-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动定时清理任务
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            int intervalMinutes = properties.getCleanupIntervalMinutes();
            cleanupExecutor.scheduleAtFixedRate(
                    this::cleanup,
                    intervalMinutes,
                    intervalMinutes,
                    TimeUnit.MINUTES
            );
            log.info("ErrorDetailStore started with TTL={}min, cleanup interval={}min",
                    properties.getTtlMinutes(), intervalMinutes);
        }
    }

    /**
     * 停止清理任务
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("ErrorDetailStore stopped");
        }
    }

    /**
     * 存储错误详情
     *
     * @param detail 错误详情
     * @return 存储成功返回 true，如果已达到最大数量返回 false
     */
    public boolean store(ErrorDetail detail) {
        if (detail == null || detail.getErrorId() == null) {
            return false;
        }

        // 检查是否达到最大数量
        if (store.size() >= properties.getMaxStoredErrors()) {
            log.warn("ErrorDetailStore reached max capacity ({}), cleaning up expired entries first",
                    properties.getMaxStoredErrors());
            cleanup();

            // 再次检查
            if (store.size() >= properties.getMaxStoredErrors()) {
                log.error("ErrorDetailStore still at max capacity after cleanup, rejecting new error");
                return false;
            }
        }

        // 设置过期时间
        if (detail.getExpiresAt() == null) {
            detail.setExpiresAt(Instant.now().plusSeconds(properties.getTtlMinutes() * 60L));
        }

        store.put(detail.getErrorId(), detail);
        log.debug("Stored error detail: errorId={}, expiresAt={}",
                detail.getErrorId(), detail.getExpiresAt());
        return true;
    }

    /**
     * 根据 errorId 获取错误详情
     *
     * @param errorId 错误 ID
     * @return 错误详情，如果不存在或已过期返回 empty
     */
    public Optional<ErrorDetail> get(String errorId) {
        if (errorId == null) {
            return Optional.empty();
        }

        ErrorDetail detail = store.get(errorId);
        if (detail == null) {
            return Optional.empty();
        }

        // 检查是否过期
        if (detail.getExpiresAt() != null && Instant.now().isAfter(detail.getExpiresAt())) {
            store.remove(errorId);
            log.debug("Error detail expired and removed: errorId={}", errorId);
            return Optional.empty();
        }

        return Optional.of(detail);
    }

    /**
     * 根据 errorId 删除错误详情
     *
     * @param errorId 错误 ID
     * @return 如果存在并删除返回 true
     */
    public boolean remove(String errorId) {
        return store.remove(errorId) != null;
    }

    /**
     * 清理过期的错误详情
     *
     * @return 清理的数量
     */
    public int cleanup() {
        Instant now = Instant.now();
        List<String> expiredIds = new ArrayList<>();

        for (Map.Entry<String, ErrorDetail> entry : store.entrySet()) {
            ErrorDetail detail = entry.getValue();
            if (detail.getExpiresAt() != null && now.isAfter(detail.getExpiresAt())) {
                expiredIds.add(entry.getKey());
            }
        }

        for (String id : expiredIds) {
            store.remove(id);
        }

        if (!expiredIds.isEmpty()) {
            log.info("Cleaned up {} expired error details, remaining: {}", expiredIds.size(), store.size());
        }

        return expiredIds.size();
    }

    /**
     * 获取当前存储的错误数量
     */
    public int size() {
        return store.size();
    }

    /**
     * 清空所有存储的错误
     */
    public void clear() {
        store.clear();
        log.info("ErrorDetailStore cleared");
    }

    /**
     * 获取存储状态信息
     */
    public StoreStatus getStatus() {
        return StoreStatus.builder()
                .totalCount(store.size())
                .maxCapacity(properties.getMaxStoredErrors())
                .ttlMinutes(properties.getTtlMinutes())
                .cleanupIntervalMinutes(properties.getCleanupIntervalMinutes())
                .build();
    }

    /**
     * 存储状态信息
     */
    @lombok.Data
    @lombok.Builder
    public static class StoreStatus {
        private int totalCount;
        private int maxCapacity;
        private int ttlMinutes;
        private int cleanupIntervalMinutes;
    }

}

