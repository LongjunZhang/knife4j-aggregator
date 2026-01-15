package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.config.ErrorCollectorProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.RequestContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 请求上下文缓存服务
 * 缓存接口调用的请求和响应信息，用于 AI 错误分析
 */
@Slf4j
@Service
public class RequestContextCacheService {

    private final ErrorCollectorProxyProperties properties;
    private final Map<String, RequestContext> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public RequestContextCacheService(ErrorCollectorProxyProperties properties) {
        this.properties = properties;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "request-context-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void init() {
        // 每 5 分钟清理一次过期的缓存
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
        log.info("RequestContextCacheService initialized with TTL={}min, maxSize={}",
                properties.getRequestCacheTtlMinutes(), properties.getMaxCachedRequests());
    }

    @PreDestroy
    public void destroy() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 生成新的请求 ID
     */
    public String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 缓存请求上下文
     *
     * @param context 请求上下文
     * @return 是否缓存成功
     */
    public boolean cache(RequestContext context) {
        if (context == null || context.getRequestId() == null) {
            return false;
        }

        // 检查容量
        if (cache.size() >= properties.getMaxCachedRequests()) {
            cleanup();
            if (cache.size() >= properties.getMaxCachedRequests()) {
                log.warn("Request context cache is full, rejecting new entry");
                return false;
            }
        }

        // 设置过期时间
        if (context.getExpiresAt() == null) {
            context.setExpiresAt(Instant.now().plusSeconds(
                    properties.getRequestCacheTtlMinutes() * 60L));
        }

        cache.put(context.getRequestId(), context);
        log.debug("Cached request context: requestId={}, hasError={}",
                context.getRequestId(), context.isHasError());

        return true;
    }

    /**
     * 获取请求上下文
     */
    public Optional<RequestContext> get(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }

        RequestContext context = cache.get(requestId);
        if (context == null) {
            return Optional.empty();
        }

        // 检查是否过期
        if (context.getExpiresAt() != null && Instant.now().isAfter(context.getExpiresAt())) {
            cache.remove(requestId);
            return Optional.empty();
        }

        return Optional.of(context);
    }

    /**
     * 根据 errorId 获取请求上下文
     */
    public Optional<RequestContext> getByErrorId(String errorId) {
        if (errorId == null) {
            return Optional.empty();
        }

        return cache.values().stream()
                .filter(ctx -> errorId.equals(ctx.getErrorId()))
                .findFirst();
    }

    /**
     * 获取服务最近的错误请求列表
     */
    public List<RequestContext> getRecentErrors(String serviceName, int limit) {
        return cache.values().stream()
                .filter(ctx -> ctx.isHasError())
                .filter(ctx -> serviceName == null || serviceName.equals(ctx.getServiceName()))
                .sorted(Comparator.comparing(RequestContext::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 删除请求上下文
     */
    public boolean remove(String requestId) {
        return cache.remove(requestId) != null;
    }

    /**
     * 清理过期的缓存
     */
    public int cleanup() {
        Instant now = Instant.now();
        List<String> expiredIds = new ArrayList<>();

        for (Map.Entry<String, RequestContext> entry : cache.entrySet()) {
            RequestContext context = entry.getValue();
            if (context.getExpiresAt() != null && now.isAfter(context.getExpiresAt())) {
                expiredIds.add(entry.getKey());
            }
        }

        for (String id : expiredIds) {
            cache.remove(id);
        }

        if (!expiredIds.isEmpty()) {
            log.info("Cleaned up {} expired request contexts, remaining: {}",
                    expiredIds.size(), cache.size());
        }

        return expiredIds.size();
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        long errorCount = cache.values().stream().filter(RequestContext::isHasError).count();
        return new CacheStats(cache.size(), errorCount, properties.getMaxCachedRequests());
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(int totalCount, long errorCount, int maxCapacity) {}

}

