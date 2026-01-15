/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.zhanglongjun.knife4j.aggregator.model.ApiDocVersion;
import com.github.zhanglongjun.knife4j.aggregator.model.SemanticVersion;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiDocVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 * 文档版本管理服务
 * 
 * 负责：
 * - hash 计算（稳定序列化后 SHA256）
 * - 幂等检测（通过 contentHash 判断）
 * - 版本递增管理
 * - 接口数量统计
 */
@Service
public class DocVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(DocVersionService.class);
    
    private final ApiDocVersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    private final Swagger2ToOAS3Converter converter;
    
    public DocVersionService(ApiDocVersionRepository versionRepository,
                             Swagger2ToOAS3Converter converter) {
        this.versionRepository = versionRepository;
        this.converter = converter;
        // 配置 ObjectMapper 以确保稳定的 JSON 序列化顺序
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }
    
    /**
     * 获取服务的最新版本
     */
    public Mono<ApiDocVersion> getLatestVersion(String serviceName) {
        return versionRepository.findTopByServiceNameOrderByVersionDesc(serviceName);
    }
    
    /**
     * 获取指定版本
     * 
     * @param version 语义化版本号，如 "1.0.0"
     */
    public Mono<ApiDocVersion> getVersion(String serviceName, String version) {
        return versionRepository.findByServiceNameAndVersion(serviceName, version);
    }
    
    /**
     * 获取版本列表（自动去重）
     */
    public Flux<ApiDocVersion> getVersionList(String serviceName) {
        return versionRepository.findByServiceNameOrderByVersionDesc(serviceName)
                .distinct(ApiDocVersion::getVersion);  // 按版本号去重，保留第一条（最新的）
    }
    
    /**
     * 获取版本列表（分页，自动去重）
     */
    public Flux<ApiDocVersion> getVersionList(String serviceName, int page, int size) {
        return versionRepository.findByServiceNameOrderByVersionDesc(serviceName, PageRequest.of(page, size))
                .distinct(ApiDocVersion::getVersion);  // 按版本号去重
    }
    
    /**
     * 统计版本数量
     */
    public Mono<Long> countVersions(String serviceName) {
        return versionRepository.countByServiceName(serviceName);
    }
    
    /**
     * 计算内容哈希（标准化后的 SHA256）
     * 
     * 为确保哈希稳定：
     * 1. 使用 Swagger2ToOAS3Converter.normalizeJson 进行深度排序规范化
     * 2. 计算 SHA256
     * 
     * 这确保了相同内容（无论字段顺序）产生相同的 hash，
     * 也确保了 Swagger 2.0 转换后的 OpenAPI 3.0 文档 hash 稳定。
     */
    public String calculateContentHash(String content) {
        try {
            // 使用统一的规范化方法（深度排序所有字段）
            String normalizedJson = converter.normalizeJson(content);
            
            // 计算 SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalizedJson.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder("sha256:");
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("计算内容哈希失败", e);
            // 回退：直接对原始内容计算哈希
            return "sha256:" + content.hashCode();
        }
    }
    
    /**
     * 检查内容是否已存在（幂等检测）
     */
    public Mono<Boolean> isContentExists(String serviceName, String contentHash) {
        return versionRepository.findByServiceNameAndContentHash(serviceName, contentHash)
                .hasElement();
    }
    
    /**
     * 统计 OpenAPI 文档中的接口数量
     */
    public int countApis(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode paths = root.get("paths");
            
            if (paths == null || !paths.isObject()) {
                return 0;
            }
            
            int count = 0;
            Iterator<String> pathIterator = paths.fieldNames();
            while (pathIterator.hasNext()) {
                String path = pathIterator.next();
                JsonNode pathNode = paths.get(path);
                if (pathNode != null && pathNode.isObject()) {
                    Iterator<String> methodIterator = pathNode.fieldNames();
                    while (methodIterator.hasNext()) {
                        String method = methodIterator.next();
                        // 排除非 HTTP 方法的字段（如 parameters, summary 等）
                        if (isHttpMethod(method)) {
                            count++;
                        }
                    }
                }
            }
            
            return count;
        } catch (JsonProcessingException e) {
            log.error("统计接口数量失败", e);
            return 0;
        }
    }
    
    /**
     * 判断是否为 HTTP 方法
     */
    private boolean isHttpMethod(String method) {
        String upper = method.toUpperCase();
        return "GET".equals(upper) || "POST".equals(upper) || "PUT".equals(upper) 
                || "DELETE".equals(upper) || "PATCH".equals(upper) || "HEAD".equals(upper) 
                || "OPTIONS".equals(upper) || "TRACE".equals(upper);
    }
    
    /**
     * 创建初始版本
     */
    public Mono<ApiDocVersion> createInitialVersion(String serviceName, String content) {
        String contentHash = calculateContentHash(content);
        int apiCount = countApis(content);
        
        ApiDocVersion version = ApiDocVersion.createInitial(serviceName, content, contentHash, apiCount);
        
        log.info("创建服务 {} 的初始版本，接口数量: {}", serviceName, apiCount);
        
        return versionRepository.save(version);
    }
    
    /**
     * 创建新版本
     * 注意：此方法使用简单递增逻辑，建议通过 DocPersistenceService.persistDocument 来创建版本
     */
    public Mono<ApiDocVersion> createNewVersion(String serviceName, String content, 
                                                 String changeType, String changeSummary) {
        String contentHash = calculateContentHash(content);
        int apiCount = countApis(content);
        
        return getLatestVersion(serviceName)
                .map(latest -> {
                    // 解析当前版本并默认升级 patch
                    SemanticVersion current = parseVersionString(latest.getVersion());
                    return current.bumpPatch().toString();
                })
                .defaultIfEmpty(SemanticVersion.initial().toString())
                .flatMap(newVersionStr -> {
                    ApiDocVersion version = ApiDocVersion.createNew(
                            serviceName, newVersionStr, content, contentHash, 
                            apiCount, changeType, changeSummary);
                    
                    log.info("创建服务 {} 的新版本 v{}，变更类型: {}，接口数量: {}", 
                            serviceName, newVersionStr, changeType, apiCount);
                    
                    return versionRepository.save(version);
                });
    }
    
    /**
     * 解析版本号字符串，兼容整数和语义化版本
     */
    private SemanticVersion parseVersionString(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return SemanticVersion.initial();
        }
        
        SemanticVersion semVer = SemanticVersion.tryParse(versionStr);
        if (semVer != null) {
            return semVer;
        }
        
        try {
            int intVersion = Integer.parseInt(versionStr);
            return SemanticVersion.fromIntVersion(intVersion);
        } catch (NumberFormatException e) {
            return SemanticVersion.initial();
        }
    }
    
    /**
     * 保存或更新版本（带幂等检测）
     * 
     * 如果内容未变化，返回 null（表示无需新版本）
     * 如果是新内容，返回创建的新版本
     */
    public Mono<ApiDocVersion> saveVersionIfChanged(String serviceName, String content,
                                                     String changeType, String changeSummary) {
        String contentHash = calculateContentHash(content);
        
        return isContentExists(serviceName, contentHash)
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("服务 {} 文档内容未变化，跳过版本创建", serviceName);
                        return Mono.empty();
                    }
                    
                    return getLatestVersion(serviceName)
                            .flatMap(latest -> createNewVersion(serviceName, content, changeType, changeSummary))
                            .switchIfEmpty(createInitialVersion(serviceName, content));
                });
    }
}

