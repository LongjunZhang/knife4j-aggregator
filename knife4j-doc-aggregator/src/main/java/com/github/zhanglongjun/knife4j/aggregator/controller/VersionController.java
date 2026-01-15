/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.controller;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiChange;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiDocVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.ServiceInfo;
import com.github.zhanglongjun.knife4j.aggregator.model.SyncLog;
import com.github.zhanglongjun.knife4j.aggregator.service.DocDiffService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 版本管理控制器
 * 
 * 提供文档版本相关的 API：
 * - 版本列表查询
 * - 指定版本查询
 * - 版本对比（Diff）
 * - 手动同步
 * - 变更记录查询
 * - 同步日志查询
 */
@RestController
@RequestMapping("/api")
public class VersionController {
    
    private static final Logger log = LoggerFactory.getLogger(VersionController.class);
    
    private final DocPersistenceService persistenceService;
    private final Knife4jAggregatorProperties properties;
    
    public VersionController(DocPersistenceService persistenceService,
                            Knife4jAggregatorProperties properties) {
        this.persistenceService = persistenceService;
        this.properties = properties;
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
    
    // ==================== 服务管理 ====================
    
    /**
     * 获取所有服务列表
     * GET /api/services
     */
    @GetMapping("/services")
    public Flux<ServiceInfo> getAllServices() {
        return persistenceService.getAllServices();
    }
    
    /**
     * 获取单个服务信息
     * GET /api/services/{serviceName}
     */
    @GetMapping("/services/{serviceName}")
    public Mono<ResponseEntity<ServiceInfo>> getServiceInfo(@PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.getServiceInfo(normalized)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    // ==================== 文档查询 ====================
    
    /**
     * 获取服务的最新文档
     * GET /api/docs/{serviceName}
     */
    @GetMapping("/docs/{serviceName}")
    public Mono<ResponseEntity<String>> getLatestDoc(@PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.getLatestContent(normalized)
                .map(content -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(content))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    // ==================== 版本管理 ====================
    
    /**
     * 获取服务的版本列表（确保按版本号去重）
     * GET /api/docs/{serviceName}/versions
     */
    @GetMapping("/docs/{serviceName}/versions")
    public Flux<VersionSummary> getVersionList(
            @PathVariable("serviceName") String serviceName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.getVersionList(normalized, page, size)
                .distinct(ApiDocVersion::getVersion)  // 双重保险：Controller 层再次去重
                .map(VersionSummary::from);
    }
    
    /**
     * 获取指定版本的文档
     * GET /api/docs/{serviceName}/versions/{version}
     * 
     * @param version 语义化版本号，如 "1.0.0"
     */
    @GetMapping("/docs/{serviceName}/versions/{version}")
    public Mono<ResponseEntity<ApiDocVersion>> getVersion(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("version") String version) {
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.getVersionList(normalized)
                .filter(v -> v.getVersion().equals(version))
                .next()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取指定版本的变更明细
     * GET /api/docs/{serviceName}/versions/{version}/changes
     * 
     * @param version 语义化版本号，如 "1.0.0"
     */
    @GetMapping("/docs/{serviceName}/versions/{version}/changes")
    public Flux<ApiChange> getVersionChanges(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("version") String version) {
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.getVersionChanges(normalized, version);
    }
    
    /**
     * 对比两个版本
     * GET /api/docs/{serviceName}/diff?v1={v1}&v2={v2}
     * 
     * @param v1 版本1，如 "1.0.0"
     * @param v2 版本2，如 "1.1.0"
     */
    @GetMapping("/docs/{serviceName}/diff")
    public Mono<ResponseEntity<DiffResponse>> compareVersions(
            @PathVariable("serviceName") String serviceName,
            @RequestParam("v1") String v1,
            @RequestParam("v2") String v2) {
        
        if (v1.equals(v2)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(DiffResponse.error("v1 和 v2 不能相同")));
        }
        
        String normalized = normalizeServiceName(serviceName);
        return persistenceService.compareVersions(normalized, v1, v2)
                .map(result -> ResponseEntity.ok(DiffResponse.from(result, v1, v2)))
                .onErrorResume(e -> {
                    log.error("对比版本失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(DiffResponse.error(e.getMessage())));
                });
    }
    
    // ==================== 同步控制 ====================
    
    /**
     * 手动同步单个服务
     * POST /api/sync/{serviceName}
     */
    @PostMapping("/sync/{serviceName}")
    public Mono<ResponseEntity<SyncResponse>> syncService(@PathVariable("serviceName") String serviceName) {
        String normalized = normalizeServiceName(serviceName);
        log.info("手动同步服务: {} (normalized: {})", serviceName, normalized);
        
        // 注意：这里只是触发持久化，实际的 fetch 需要通过 CacheManager
        // 返回当前版本信息
        return persistenceService.getServiceInfo(normalized)
                .map(info -> ResponseEntity.ok(SyncResponse.success(
                        normalized, 
                        info.getCurrentVersion(), 
                        "同步请求已接收，请查看最新版本")))
                .defaultIfEmpty(ResponseEntity.ok(SyncResponse.success(
                        normalized, null, "服务尚无版本记录")));
    }
    
    // ==================== 变更与日志 ====================
    
    /**
     * 获取最近的变更记录
     * GET /api/changes
     */
    @GetMapping("/changes")
    public Flux<ApiChange> getRecentChanges(
            @RequestParam(name = "serviceName", required = false) String serviceName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        if (serviceName != null && !serviceName.isEmpty()) {
            String normalized = normalizeServiceName(serviceName);
            return persistenceService.getVersionChanges(normalized, "0.0.0"); // TODO: 实现分页
        }
        // 返回所有服务的最近变更
        return persistenceService.getAllServices()
                .flatMap(service -> persistenceService.getVersionChanges(service.getServiceName(), 
                        service.getCurrentVersion() != null ? service.getCurrentVersion() : "1.0.0"))
                .take(size);
    }
    
    /**
     * 获取同步日志
     * GET /api/sync/logs
     */
    @GetMapping("/sync/logs")
    public Flux<SyncLog> getSyncLogs(
            @RequestParam(name = "serviceName", required = false) String serviceName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        if (serviceName != null && !serviceName.isEmpty()) {
            String normalized = normalizeServiceName(serviceName);
            return persistenceService.getSyncLogs(normalized, page, size);
        }
        // 返回所有服务的日志
        return persistenceService.getAllServices()
                .flatMap(service -> persistenceService.getSyncLogs(service.getServiceName(), 0, 10))
                .take(size);
    }
    
    // ==================== 响应 DTO ====================
    
    /**
     * 版本摘要（不包含完整内容）
     */
    public static class VersionSummary {
        private String version; // 语义化版本号，如 "1.0.0"
        private String contentHash;
        private Integer apiCount;
        private String changeType;
        private String changeSummary;
        private String createdAt;
        
        public static VersionSummary from(ApiDocVersion v) {
            VersionSummary summary = new VersionSummary();
            summary.version = v.getVersion();
            summary.contentHash = v.getContentHash();
            summary.apiCount = v.getApiCount();
            summary.changeType = v.getChangeType();
            summary.changeSummary = v.getChangeSummary();
            summary.createdAt = v.getCreatedAt() != null ? v.getCreatedAt().toString() : null;
            return summary;
        }
        
        public String getVersion() { return version; }
        public String getContentHash() { return contentHash; }
        public Integer getApiCount() { return apiCount; }
        public String getChangeType() { return changeType; }
        public String getChangeSummary() { return changeSummary; }
        public String getCreatedAt() { return createdAt; }
    }
    
    /**
     * Diff 响应
     */
    public static class DiffResponse {
        private boolean success;
        private String error;
        private String v1; // 语义化版本号
        private String v2; // 语义化版本号
        private String summary;
        private int addedCount;
        private int removedCount;
        private int modifiedCount;
        private java.util.List<ApiChange> changes;
        
        public static DiffResponse from(DocDiffService.DiffResult result, String v1, String v2) {
            DiffResponse response = new DiffResponse();
            response.success = true;
            response.v1 = v1;
            response.v2 = v2;
            response.summary = result.getSummary();
            response.addedCount = result.getAddedCount();
            response.removedCount = result.getRemovedCount();
            response.modifiedCount = result.getModifiedCount();
            response.changes = result.getChanges();
            return response;
        }
        
        public static DiffResponse error(String error) {
            DiffResponse response = new DiffResponse();
            response.success = false;
            response.error = error;
            return response;
        }
        
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public String getV1() { return v1; }
        public String getV2() { return v2; }
        public String getSummary() { return summary; }
        public int getAddedCount() { return addedCount; }
        public int getRemovedCount() { return removedCount; }
        public int getModifiedCount() { return modifiedCount; }
        public java.util.List<ApiChange> getChanges() { return changes; }
    }
    
    /**
     * 同步响应
     */
    public static class SyncResponse {
        private boolean success;
        private String serviceName;
        private String currentVersion; // 语义化版本号
        private String message;
        
        public static SyncResponse success(String serviceName, String version, String message) {
            SyncResponse response = new SyncResponse();
            response.success = true;
            response.serviceName = serviceName;
            response.currentVersion = version;
            response.message = message;
            return response;
        }
        
        public boolean isSuccess() { return success; }
        public String getServiceName() { return serviceName; }
        public String getCurrentVersion() { return currentVersion; }
        public String getMessage() { return message; }
    }
}

