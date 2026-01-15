/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.github.zhanglongjun.knife4j.aggregator.model.ApiChange;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiDocVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.SemanticVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.SemanticVersion.ChangeLevel;
import com.github.zhanglongjun.knife4j.aggregator.model.ServiceInfo;
import com.github.zhanglongjun.knife4j.aggregator.model.SyncLog;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiChangeRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiDocVersionRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.ServiceInfoRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 文档持久化服务
 * 
 * 协调版本管理、Diff 对比、日志记录等功能
 * 提供统一的持久化入口
 */
@Service
public class DocPersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(DocPersistenceService.class);
    
    private final DocVersionService versionService;
    private final DocDiffService diffService;
    private final ServiceInfoRepository serviceInfoRepository;
    private final ApiDocVersionRepository versionRepository;
    private final ApiChangeRepository changeRepository;
    private final SyncLogRepository syncLogRepository;
    
    public DocPersistenceService(
            DocVersionService versionService,
            DocDiffService diffService,
            ServiceInfoRepository serviceInfoRepository,
            ApiDocVersionRepository versionRepository,
            ApiChangeRepository changeRepository,
            SyncLogRepository syncLogRepository) {
        this.versionService = versionService;
        this.diffService = diffService;
        this.serviceInfoRepository = serviceInfoRepository;
        this.versionRepository = versionRepository;
        this.changeRepository = changeRepository;
        this.syncLogRepository = syncLogRepository;
    }
    
    /**
     * 持久化结果
     */
    public static class PersistResult {
        private final boolean hasNewVersion;
        private final String newVersion; // 语义化版本号，如 "1.0.0"
        private final String changeSummary;
        private final int addedCount;
        private final int removedCount;
        private final int modifiedCount;
        
        private PersistResult(boolean hasNewVersion, String newVersion, String changeSummary,
                              int addedCount, int removedCount, int modifiedCount) {
            this.hasNewVersion = hasNewVersion;
            this.newVersion = newVersion;
            this.changeSummary = changeSummary;
            this.addedCount = addedCount;
            this.removedCount = removedCount;
            this.modifiedCount = modifiedCount;
        }
        
        public static PersistResult noChange() {
            return new PersistResult(false, null, "无变更", 0, 0, 0);
        }
        
        public static PersistResult newVersion(String version, String summary, 
                                                int added, int removed, int modified) {
            return new PersistResult(true, version, summary, added, removed, modified);
        }
        
        public boolean hasNewVersion() { return hasNewVersion; }
        public String getNewVersion() { return newVersion; }
        public String getChangeSummary() { return changeSummary; }
        public int getAddedCount() { return addedCount; }
        public int getRemovedCount() { return removedCount; }
        public int getModifiedCount() { return modifiedCount; }
    }
    
    /**
     * 持久化文档（核心方法）
     * 
     * 流程：
     * 1. 计算新内容的 hash
     * 2. 检查是否与最新版本相同（幂等）
     * 3. 如果有变化，判断变更级别（MAJOR/MINOR/PATCH）
     * 4. 计算新的语义化版本号
     * 5. 保存新版本
     * 6. 更新服务信息
     * 7. 记录同步日志
     */
    public Mono<PersistResult> persistDocument(String serviceName, String newContent) {
        long startTime = System.currentTimeMillis();
        String newHash = versionService.calculateContentHash(newContent);
        
        return versionService.getLatestVersion(serviceName)
                .flatMap(latestVersion -> {
                    // 幂等检查：内容未变化
                    if (latestVersion.getContentHash().equals(newHash)) {
                        log.debug("服务 {} 文档内容未变化，跳过持久化", serviceName);
                        long duration = System.currentTimeMillis() - startTime;
                        return syncLogRepository.save(SyncLog.noChange(serviceName, duration))
                                .thenReturn(PersistResult.noChange());
                    }
                    
                    // 有变化，判断变更级别并计算新版本号
                    ChangeLevel changeLevel = diffService.determineChangeLevel(
                            latestVersion.getContent(), newContent);
                    
                    // 解析当前版本号并升级
                    SemanticVersion currentVersion = parseVersion(latestVersion.getVersion());
                    SemanticVersion newVersion = currentVersion.bump(changeLevel);
                    String newVersionStr = newVersion.toString();
                    
                    log.info("服务 {} 变更级别: {}, 版本升级: {} -> {}", 
                            serviceName, changeLevel, latestVersion.getVersion(), newVersionStr);
                    
                    // 执行详细 Diff 用于生成变更记录
                    DocDiffService.DiffResult diffResult = diffService.diff(
                            latestVersion.getContent(), newContent, serviceName, newVersionStr);
                    
                    return saveNewVersion(serviceName, newContent, newHash, newVersionStr, diffResult, startTime);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 首次创建版本
                    log.info("服务 {} 首次创建文档版本 1.0.0", serviceName);
                    return saveInitialVersion(serviceName, newContent, newHash, startTime);
                }));
    }
    
    /**
     * 解析版本号字符串
     * 兼容旧的整数版本和新的语义化版本
     */
    private SemanticVersion parseVersion(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return SemanticVersion.initial();
        }
        
        // 尝试解析语义化版本
        SemanticVersion semVer = SemanticVersion.tryParse(versionStr);
        if (semVer != null) {
            return semVer;
        }
        
        // 尝试解析为整数（兼容旧数据）
        try {
            int intVersion = Integer.parseInt(versionStr);
            return SemanticVersion.fromIntVersion(intVersion);
        } catch (NumberFormatException e) {
            log.warn("无法解析版本号: {}, 使用初始版本", versionStr);
            return SemanticVersion.initial();
        }
    }
    
    /**
     * 保存初始版本 (1.0.0)
     */
    private Mono<PersistResult> saveInitialVersion(String serviceName, String content, 
                                                    String contentHash, long startTime) {
        int apiCount = versionService.countApis(content);
        
        ApiDocVersion initialVersion = ApiDocVersion.createInitial(serviceName, content, contentHash, apiCount);
        
        return versionRepository.save(initialVersion)
                .flatMap(savedVersion -> {
                    // 确保服务信息存在
                    return ensureServiceInfo(serviceName, savedVersion.getVersion())
                            .then(Mono.defer(() -> {
                                long duration = System.currentTimeMillis() - startTime;
                                SyncLog syncLog = SyncLog.success(serviceName, duration, 
                                        savedVersion.getVersion(), "初始版本");
                                return syncLogRepository.save(syncLog);
                            }))
                            .thenReturn(PersistResult.newVersion(
                                    savedVersion.getVersion(), "初始版本", apiCount, 0, 0));
                });
    }
    
    /**
     * 保存新版本（带 Diff）
     * 
     * @param newVersionStr 语义化版本号字符串，如 "1.1.0"
     */
    private Mono<PersistResult> saveNewVersion(String serviceName, String content, String contentHash,
                                                String newVersionStr, DocDiffService.DiffResult diffResult,
                                                long startTime) {
        int apiCount = versionService.countApis(content);
        
        ApiDocVersion newVersion = ApiDocVersion.createNew(
                serviceName, newVersionStr, content, contentHash, apiCount,
                diffResult.getChangeType(), diffResult.getSummary());
        
        return versionRepository.save(newVersion)
                .flatMap(savedVersion -> {
                    // 保存变更记录
                    List<ApiChange> changes = diffResult.getChanges();
                    
                    Mono<Void> saveChanges = Mono.empty();
                    if (!changes.isEmpty()) {
                        saveChanges = changeRepository.saveAll(changes).then();
                    }
                    
                    return saveChanges
                            .then(updateServiceInfo(serviceName, savedVersion.getVersion()))
                            .then(Mono.defer(() -> {
                                long duration = System.currentTimeMillis() - startTime;
                                SyncLog syncLog = SyncLog.success(serviceName, duration, 
                                        savedVersion.getVersion(), diffResult.getSummary());
                                return syncLogRepository.save(syncLog);
                            }))
                            .thenReturn(PersistResult.newVersion(
                                    savedVersion.getVersion(), 
                                    diffResult.getSummary(),
                                    diffResult.getAddedCount(),
                                    diffResult.getRemovedCount(),
                                    diffResult.getModifiedCount()));
                });
    }
    
    /**
     * 确保服务信息存在
     */
    private Mono<ServiceInfo> ensureServiceInfo(String serviceName, String currentVersion) {
        return serviceInfoRepository.findFirstByServiceName(serviceName)
                .flatMap(info -> {
                    info.updateSyncStatus("SUCCESS", currentVersion);
                    return serviceInfoRepository.save(info);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ServiceInfo newInfo = ServiceInfo.create(serviceName, serviceName, "/v3/api-docs");
                    newInfo.updateSyncStatus("SUCCESS", currentVersion);
                    return serviceInfoRepository.save(newInfo);
                }));
    }
    
    /**
     * 更新服务信息
     */
    private Mono<Void> updateServiceInfo(String serviceName, String currentVersion) {
        return serviceInfoRepository.findFirstByServiceName(serviceName)
                .flatMap(info -> {
                    info.updateSyncStatus("SUCCESS", currentVersion);
                    return serviceInfoRepository.save(info);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ServiceInfo newInfo = ServiceInfo.create(serviceName, serviceName, "/v3/api-docs");
                    newInfo.updateSyncStatus("SUCCESS", currentVersion);
                    return serviceInfoRepository.save(newInfo);
                }))
                .then();
    }
    
    /**
     * 获取服务的最新文档内容（从 MongoDB）
     */
    public Mono<String> getLatestContent(String serviceName) {
        return versionService.getLatestVersion(serviceName)
                .map(ApiDocVersion::getContent);
    }
    
    /**
     * 获取指定版本的文档内容
     * 
     * @param version 语义化版本号，如 "1.0.0"
     */
    public Mono<String> getVersionContent(String serviceName, String version) {
        return versionService.getVersion(serviceName, version)
                .map(ApiDocVersion::getContent);
    }
    
    /**
     * 获取版本列表
     */
    public Flux<ApiDocVersion> getVersionList(String serviceName) {
        return versionService.getVersionList(serviceName);
    }
    
    /**
     * 获取版本列表（分页）
     */
    public Flux<ApiDocVersion> getVersionList(String serviceName, int page, int size) {
        return versionService.getVersionList(serviceName, page, size);
    }
    
    /**
     * 获取指定版本的变更明细
     * 
     * @param version 语义化版本号，如 "1.0.0"
     */
    public Flux<ApiChange> getVersionChanges(String serviceName, String version) {
        return changeRepository.findByServiceNameAndVersion(serviceName, version);
    }
    
    /**
     * 对比两个版本
     * 
     * @param v1 版本1，如 "1.0.0"
     * @param v2 版本2，如 "1.1.0"
     */
    public Mono<DocDiffService.DiffResult> compareVersions(String serviceName, String v1, String v2) {
        Mono<ApiDocVersion> version1 = versionService.getVersion(serviceName, v1);
        Mono<ApiDocVersion> version2 = versionService.getVersion(serviceName, v2);
        
        return Mono.zip(version1, version2)
                .map(tuple -> diffService.diffVersions(
                        tuple.getT1().getContent(), 
                        tuple.getT2().getContent(), 
                        serviceName));
    }
    
    /**
     * 记录同步失败日志
     */
    public Mono<SyncLog> logSyncFailed(String serviceName, long durationMs, String errorMessage) {
        return serviceInfoRepository.findFirstByServiceName(serviceName)
                .flatMap(info -> {
                    info.updateSyncStatus("FAILED", null);
                    return serviceInfoRepository.save(info);
                })
                .then(syncLogRepository.save(SyncLog.failed(serviceName, durationMs, errorMessage)));
    }
    
    /**
     * 获取同步日志
     */
    public Flux<SyncLog> getSyncLogs(String serviceName, int page, int size) {
        return syncLogRepository.findByServiceNameOrderByCreatedAtDesc(
                serviceName, org.springframework.data.domain.PageRequest.of(page, size));
    }
    
    /**
     * 获取所有服务信息
     */
    public Flux<ServiceInfo> getAllServices() {
        return serviceInfoRepository.findAllByOrderByOrderAsc();
    }
    
    /**
     * 获取服务信息
     */
    public Mono<ServiceInfo> getServiceInfo(String serviceName) {
        return serviceInfoRepository.findFirstByServiceName(serviceName);
    }
    
    /**
     * 检查是否有持久化的文档
     */
    public Mono<Boolean> hasPersistedContent(String serviceName) {
        return versionService.getLatestVersion(serviceName).hasElement();
    }
}

