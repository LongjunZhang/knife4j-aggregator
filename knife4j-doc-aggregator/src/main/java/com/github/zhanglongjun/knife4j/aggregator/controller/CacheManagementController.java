/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.controller;

import com.github.zhanglongjun.knife4j.aggregator.cache.ApiDocsCacheManager;
import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiDocVersion;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiChangeRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiDocVersionRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存管理接口
 * 
 * 提供文档缓存的手动刷新和状态查询功能
 */
@RestController
@RequestMapping("/api/cache")
public class CacheManagementController {
    
    private static final Logger log = LoggerFactory.getLogger(CacheManagementController.class);
    
    private final ApiDocsCacheManager cacheManager;
    private final ApiDocVersionRepository versionRepository;
    private final ApiChangeRepository changeRepository;
    private final SyncLogRepository syncLogRepository;
    private final Knife4jAggregatorProperties properties;
    
    public CacheManagementController(ApiDocsCacheManager cacheManager, 
                                      ApiDocVersionRepository versionRepository,
                                      ApiChangeRepository changeRepository,
                                      SyncLogRepository syncLogRepository,
                                      Knife4jAggregatorProperties properties) {
        this.cacheManager = cacheManager;
        this.versionRepository = versionRepository;
        this.changeRepository = changeRepository;
        this.syncLogRepository = syncLogRepository;
        this.properties = properties;
        log.info("缓存管理接口已初始化");
    }
    
    /**
     * 将路径中的 serviceName 规范化为服务 ID（根据 context-path 反查）
     * 例如：orderService -> order-service, messageService -> message-service
     */
    private String normalizeServiceName(String pathServiceName) {
        Map<String, String> ctxMap = properties.getDiscover().getServiceContextPaths();
        if (ctxMap != null) {
            String pathSeg = pathServiceName.startsWith("/") ? pathServiceName : "/" + pathServiceName;
            for (Map.Entry<String, String> e : ctxMap.entrySet()) {
                String ctx = e.getValue();
                if (ctx == null || ctx.isEmpty()) continue;
                String normCtx = ctx.startsWith("/") ? ctx : "/" + ctx;
                if (normCtx.equalsIgnoreCase(pathSeg)) {
                    return e.getKey(); // 返回 serviceId（如 order-service）
                }
            }
        }
        return pathServiceName; // 回退
    }
    
    /**
     * 刷新指定服务的文档缓存
     */
    @PostMapping("/refresh/{serviceName}")
    public ResponseEntity<Map<String, Object>> refreshService(@PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.info("收到刷新服务 {} (normalized: {}) 文档缓存的请求", serviceName, normalized);
        
        try {
            cacheManager.refreshServiceDoc(normalized);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "刷新成功");
            result.put("service", normalized);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("刷新服务 {} 文档缓存失败", normalized, e);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "刷新失败: " + e.getMessage());
            result.put("service", normalized);
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 刷新所有服务的文档缓存
     */
    @PostMapping("/refresh-all")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        log.info("收到刷新所有服务文档缓存的请求");
        
        try {
            cacheManager.refreshAllDocs();
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "全部刷新请求已提交");
            result.put("cachedServices", cacheManager.getCachedServiceNames());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("刷新所有服务文档缓存失败", e);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "刷新失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 查看缓存状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.debug("收到查看缓存状态的请求");
        
        Map<String, Object> status = cacheManager.getCacheStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 查看指定服务的缓存状态
     */
    @GetMapping("/status/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceStatus(@PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.debug("收到查看服务 {} (normalized: {}) 缓存状态的请求", serviceName, normalized);
        
        Map<String, Object> fullStatus = cacheManager.getCacheStatus();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> services = 
            (List<Map<String, Object>>) fullStatus.get("services");
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceName", normalized);
        result.put("cached", cacheManager.hasCached(normalized));
        
        if (services != null) {
            for (Map<String, Object> service : services) {
                if (normalized.equals(service.get("serviceName"))) {
                    result.putAll(service);
                    break;
                }
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 清理指定服务的重复版本数据
     * 保留每个版本号最早创建的那条记录，删除重复的
     */
    @DeleteMapping("/cleanup-duplicates/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> cleanupDuplicates(
            @PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.info("收到清理服务 {} (normalized: {}) 重复版本数据的请求", serviceName, normalized);
        
        return versionRepository.findByServiceNameOrderByVersionDesc(normalized)
                .collectList()
                .flatMap(allVersions -> {
                    // 按版本号分组，每组只保留第一条（最早创建的）
                    Map<String, List<ApiDocVersion>> groupedByVersion = allVersions.stream()
                            .collect(Collectors.groupingBy(ApiDocVersion::getVersion));
                    
                    List<String> idsToDelete = new ArrayList<>();
                    int duplicateCount = 0;
                    
                    for (Map.Entry<String, List<ApiDocVersion>> entry : groupedByVersion.entrySet()) {
                        List<ApiDocVersion> versions = entry.getValue();
                        if (versions.size() > 1) {
                            // 按创建时间排序，保留最早的一条
                            versions.sort(Comparator.comparing(ApiDocVersion::getCreatedAt));
                            // 跳过第一条，删除其余的
                            for (int i = 1; i < versions.size(); i++) {
                                idsToDelete.add(versions.get(i).getId());
                                duplicateCount++;
                            }
                        }
                    }
                    
                    if (idsToDelete.isEmpty()) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("success", true);
                        result.put("message", "没有发现重复数据");
                        result.put("service", normalized);
                        result.put("deletedCount", 0);
                        return Mono.just(ResponseEntity.ok(result));
                    }
                    
                    log.info("服务 {} 发现 {} 条重复数据，准备删除", normalized, duplicateCount);
                    
                    final int finalDuplicateCount = duplicateCount;
                    return versionRepository.deleteAllById(idsToDelete)
                            .then(Mono.fromCallable(() -> {
                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("success", true);
                                result.put("message", "清理完成");
                                result.put("service", normalized);
                                result.put("deletedCount", finalDuplicateCount);
                                return ResponseEntity.ok(result);
                            }));
                })
                .onErrorResume(e -> {
                    log.error("清理服务 {} 重复数据失败", normalized, e);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "清理失败: " + e.getMessage());
                    result.put("service", normalized);
                    return Mono.just(ResponseEntity.internalServerError().body(result));
                });
    }
    
    /**
     * 清理所有服务的重复版本数据
     */
    @DeleteMapping("/cleanup-duplicates-all")
    public Mono<ResponseEntity<Map<String, Object>>> cleanupAllDuplicates() {
        log.info("收到清理所有服务重复版本数据的请求");
        
        return versionRepository.findAll()
                .collectList()
                .flatMap(allVersions -> {
                    // 按 serviceName + version 分组
                    Map<String, List<ApiDocVersion>> grouped = allVersions.stream()
                            .collect(Collectors.groupingBy(v -> v.getServiceName() + ":" + v.getVersion()));
                    
                    List<String> idsToDelete = new ArrayList<>();
                    
                    for (List<ApiDocVersion> versions : grouped.values()) {
                        if (versions.size() > 1) {
                            versions.sort(Comparator.comparing(ApiDocVersion::getCreatedAt));
                            for (int i = 1; i < versions.size(); i++) {
                                idsToDelete.add(versions.get(i).getId());
                            }
                        }
                    }
                    
                    if (idsToDelete.isEmpty()) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("success", true);
                        result.put("message", "没有发现重复数据");
                        result.put("deletedCount", 0);
                        return Mono.just(ResponseEntity.ok(result));
                    }
                    
                    log.info("发现 {} 条重复数据，准备删除", idsToDelete.size());
                    
                    final int deletedCount = idsToDelete.size();
                    return versionRepository.deleteAllById(idsToDelete)
                            .then(Mono.fromCallable(() -> {
                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("success", true);
                                result.put("message", "清理完成");
                                result.put("deletedCount", deletedCount);
                                return ResponseEntity.ok(result);
                            }));
                })
                .onErrorResume(e -> {
                    log.error("清理所有重复数据失败", e);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", false);
                    result.put("message", "清理失败: " + e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(result));
                });
    }
    
    /**
     * 删除指定服务的所有版本数据（包括版本、变更记录、同步日志）
     * 
     * 用于策略 A 实施后清理历史数据，让系统重新拉取并持久化
     */
    @DeleteMapping("/versions/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteServiceVersions(
            @PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.info("收到删除服务 {} (normalized: {}) 所有版本数据的请求", serviceName, normalized);
        
        // 先统计要删除的数据量
        return Mono.zip(
                versionRepository.countByServiceName(normalized),
                changeRepository.countByServiceName(normalized),
                syncLogRepository.countByServiceName(normalized)
        ).flatMap(counts -> {
            long versionCount = counts.getT1();
            long changeCount = counts.getT2();
            long logCount = counts.getT3();
            
            log.info("服务 {} 待删除数据: {} 个版本, {} 条变更记录, {} 条同步日志",
                    normalized, versionCount, changeCount, logCount);
            
            // 并行删除所有相关数据
            return Mono.when(
                    versionRepository.deleteByServiceName(normalized),
                    changeRepository.deleteByServiceName(normalized),
                    syncLogRepository.deleteByServiceName(normalized)
            ).then(Mono.fromCallable(() -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "删除成功，请刷新缓存以重新拉取数据");
                result.put("service", normalized);
                result.put("deletedVersions", versionCount);
                result.put("deletedChanges", changeCount);
                result.put("deletedLogs", logCount);
                
                log.info("服务 {} 所有版本数据已删除", normalized);
                return ResponseEntity.ok(result);
            }));
        }).onErrorResume(e -> {
            log.error("删除服务 {} 版本数据失败", normalized, e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            result.put("service", normalized);
            return Mono.just(ResponseEntity.internalServerError().body(result));
        });
    }
    
    /**
     * 删除指定服务的所有版本数据并重新拉取
     * 
     * 组合操作：先删除历史数据，然后触发缓存刷新以重新拉取和持久化
     */
    @PostMapping("/rebuild/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> rebuildServiceVersions(
            @PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.info("收到重建服务 {} (normalized: {}) 版本数据的请求", serviceName, normalized);
        
        // 先删除历史数据
        return Mono.when(
                versionRepository.deleteByServiceName(normalized),
                changeRepository.deleteByServiceName(normalized),
                syncLogRepository.deleteByServiceName(normalized)
        ).then(Mono.fromCallable(() -> {
            log.info("服务 {} 历史数据已删除，开始刷新缓存...", normalized);
            
            // 触发缓存刷新（会重新拉取并持久化）
            try {
                cacheManager.refreshServiceDoc(normalized);
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("message", "重建成功");
                result.put("service", normalized);
                
                log.info("服务 {} 版本数据重建完成", normalized);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                log.warn("服务 {} 刷新缓存失败: {}", normalized, e.getMessage());
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("message", "历史数据已删除，但刷新缓存失败: " + e.getMessage());
                result.put("service", normalized);
                result.put("hint", "请稍后手动调用 POST /api/cache/refresh/" + normalized);
                
                return ResponseEntity.ok(result);
            }
        })).onErrorResume(e -> {
            log.error("重建服务 {} 版本数据失败", normalized, e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "重建失败: " + e.getMessage());
            result.put("service", normalized);
            return Mono.just(ResponseEntity.internalServerError().body(result));
        });
    }
}

