/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.github.zhanglongjun.knife4j.aggregator.cache.ApiDocsCacheManager;
import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.DocEndpointInfo;
import com.github.zhanglongjun.knife4j.aggregator.spec.v2.OpenAPI2Resource;
import com.github.zhanglongjun.knife4j.aggregator.utils.PathUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聚合器服务发现服务
 * 
 * 合并原来的 Knife4jAutoDiscoveryService 和 ServiceDiscoverHandler 的功能：
 * 1. 启动时自动发现所有服务并注册路由
 * 2. 通过 Nacos NamingService.subscribe() 监听服务实例变化
 * 3. 监听 Spring Cloud 心跳事件，发现新上线的服务
 * 4. 支持排除指定服务
 * 5. 集成文档缓存：服务上线时刷新缓存，服务下线时保留缓存
 * 6. 根据缓存可用性动态调整服务顺序
 * 7. 管理 gatewayResources 用于 OpenAPIEndpoint
 */
public class AggregatorDiscoveryService {
    
    private static final Logger log = LoggerFactory.getLogger(AggregatorDiscoveryService.class);
    
    /** 可用服务的基础 order 值（优先显示） */
    private static final int AVAILABLE_SERVICE_BASE_ORDER = 1;
    
    /** 不可用服务的基础 order 值（靠后显示） */
    private static final int UNAVAILABLE_SERVICE_BASE_ORDER = 1000;
    
    private final DiscoveryClient discoveryClient;
    private final Knife4jAggregatorProperties properties;
    private final NacosDiscoveryProperties nacosProperties;
    
    /** 文档缓存管理器（可选依赖） */
    private final ApiDocsCacheManager cacheManager;
    
    /** 文档端点探测服务（可选依赖） */
    private final DocEndpointProbeService endpointProbeService;
    
    /** 服务端点信息缓存 */
    private final ConcurrentHashMap<String, DocEndpointInfo> serviceEndpoints = new ConcurrentHashMap<>();
    
    /** Nacos NamingService 用于订阅服务变更 */
    private NamingService namingService;
    
    /** 聚合内容 (用于 OpenAPIEndpoint) */
    private final Set<OpenAPI2Resource> gatewayResources = new TreeSet<>();
    
    /** 已注册的服务集合 */
    private final Set<String> registeredServices = new HashSet<>();
    
    /** 服务名到 Router 的映射 */
    private final ConcurrentHashMap<String, Knife4jAggregatorProperties.Router> serviceRouterMap = new ConcurrentHashMap<>();
    
    /** 已订阅的服务集合 */
    private final ConcurrentHashMap<String, EventListener> subscribedServices = new ConcurrentHashMap<>();
    
    /** 上次心跳时的服务列表哈希 */
    private final AtomicReference<Integer> lastServicesHash = new AtomicReference<>(0);
    
    /** 是否已完成初始化 */
    private volatile boolean initialized = false;
    
    public AggregatorDiscoveryService(
            DiscoveryClient discoveryClient,
            Knife4jAggregatorProperties properties,
            NacosDiscoveryProperties nacosProperties,
            @Autowired(required = false) ApiDocsCacheManager cacheManager,
            @Autowired(required = false) DocEndpointProbeService endpointProbeService) {
        this.discoveryClient = discoveryClient;
        this.properties = properties;
        this.nacosProperties = nacosProperties;
        this.cacheManager = cacheManager;
        this.endpointProbeService = endpointProbeService;
        log.info("聚合器服务发现服务已启用，缓存管理器: {}，端点探测: {}", 
                cacheManager != null ? "已启用" : "未启用",
                endpointProbeService != null ? "已启用" : "未启用");
    }
    
    /**
     * 初始化 Nacos NamingService
     */
    @PostConstruct
    public void init() {
        try {
            Properties props = new Properties();
            props.setProperty("serverAddr", nacosProperties.getServerAddr());
            if (nacosProperties.getNamespace() != null && !nacosProperties.getNamespace().isEmpty()) {
                props.setProperty("namespace", nacosProperties.getNamespace());
            }
            if (nacosProperties.getUsername() != null && !nacosProperties.getUsername().isEmpty()) {
                props.setProperty("username", nacosProperties.getUsername());
            }
            if (nacosProperties.getPassword() != null && !nacosProperties.getPassword().isEmpty()) {
                props.setProperty("password", nacosProperties.getPassword());
            }
            
            this.namingService = NacosFactory.createNamingService(props);
            log.info("Nacos NamingService 初始化成功，serverAddr: {}", nacosProperties.getServerAddr());
        } catch (NacosException e) {
            log.error("Nacos NamingService 初始化失败", e);
        }

        // 启动即预注册：避免 UI 首次请求 /v3/api-docs/swagger-config 时 urls 为空
        // 预注册来源：knife4j.aggregator.discover.service-context-paths 的 key 集合
        preRegisterConfiguredServices();
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        for (var entry : subscribedServices.entrySet()) {
            try {
                namingService.unsubscribe(entry.getKey(), nacosProperties.getGroup(), entry.getValue());
                log.debug("已取消订阅服务: {}", entry.getKey());
            } catch (NacosException e) {
                log.warn("取消订阅服务 {} 失败", entry.getKey(), e);
            }
        }
        subscribedServices.clear();
        
        try {
            if (namingService != null) {
                namingService.shutDown();
            }
        } catch (NacosException e) {
            log.warn("关闭 NamingService 失败", e);
        }
    }
    
    /**
     * 应用启动完成后，自动发现并注册所有服务
     */
    @org.springframework.context.event.EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始自动发现 Nacos 服务...");
        
        new Thread(() -> {
                discoverAndRegisterServices();
                
                // 预热文档缓存
                warmUpDocCache();
                
                // 根据缓存状态重新排序
                reorderServicesByAvailability();
                
                initialized = true;
        }, "aggregator-discovery").start();
    }

    /**
     * 启动即预注册配置文件中显式声明的服务列表（service-context-paths 的 key）
     *
     * 目的：在真正的服务发现/心跳完成前，swagger-config 也能返回 urls 列表，
     * 避免前端首次加载时页面空白（Knife4j UI 不一定会自动重试）。
     */
    private void preRegisterConfiguredServices() {
        Map<String, String> ctx = properties.getDiscover().getServiceContextPaths();
        if (ctx == null || ctx.isEmpty()) {
            return;
        }

        int count = 0;
        for (String serviceName : ctx.keySet()) {
            if (!shouldIncludeService(serviceName)) {
                continue;
            }
            registerService(serviceName);
            // 提前订阅：服务实例变化时能更快刷新缓存与排序
            subscribeService(serviceName);
            count++;
        }

        if (count > 0) {
            log.info("已根据 service-context-paths 预注册 {} 个服务: {}", count, ctx.keySet());
        }
    }
    
    /**
     * 监听 Spring Cloud 心跳事件
     */
    @org.springframework.context.event.EventListener(HeartbeatEvent.class)
    public void onHeartbeat(HeartbeatEvent event) {
        if (!initialized) {
            return;
        }
        
        List<String> currentServices = discoveryClient.getServices();
        int currentHash = currentServices.hashCode();
        
        Integer previousHash = lastServicesHash.getAndSet(currentHash);
        if (previousHash != null && previousHash.equals(currentHash)) {
            return;
        }
        
        log.info("心跳检测到服务列表变化，当前服务数: {}", currentServices.size());
        
        int newServiceCount = 0;
        for (String serviceName : currentServices) {
            if (shouldIncludeService(serviceName) && !registeredServices.contains(serviceName)) {
                log.info("发现新服务: {}，自动注册", serviceName);
                registerService(serviceName);
                subscribeService(serviceName);
                refreshServiceDocCacheAndReorder(serviceName);
                newServiceCount++;
            }
        }
        
        if (newServiceCount > 0) {
            log.info("心跳检测完成，新注册 {} 个服务", newServiceCount);
        }
    }
    
    /**
     * 订阅服务实例变更
     */
    private void subscribeService(String serviceName) {
        if (namingService == null) {
            log.warn("NamingService 未初始化，无法订阅服务: {}", serviceName);
            return;
        }
        
        if (subscribedServices.containsKey(serviceName)) {
            return;
        }
        
        EventListener listener = event -> handleNamingEvent(serviceName, event);
        
        try {
            namingService.subscribe(serviceName, nacosProperties.getGroup(), listener);
            subscribedServices.put(serviceName, listener);
            log.info("已订阅服务 {} 的实例变更事件", serviceName);
        } catch (NacosException e) {
            log.error("订阅服务 {} 失败", serviceName, e);
        }
    }
    
    /**
     * 处理 Nacos 命名事件
     */
    private void handleNamingEvent(String serviceName, Event event) {
        if (!(event instanceof NamingEvent)) {
            return;
        }
        
        NamingEvent namingEvent = (NamingEvent) event;
        var instances = namingEvent.getInstances();
        boolean hasInstances = instances != null && !instances.isEmpty();
        
        log.info("收到服务 {} 的实例变更事件，实例数量: {}", serviceName, 
                instances != null ? instances.size() : 0);
        
        if (hasInstances) {
            log.info("检测到服务 {} 上线/重启，刷新文档缓存", serviceName);
            refreshServiceDocCacheAndReorder(serviceName);
        } else {
            log.info("检测到服务 {} 下线，保留文档缓存", serviceName);
            markServiceOffline(serviceName);
        }
    }
    
    /**
     * 发现并注册所有服务
     */
    private void discoverAndRegisterServices() {
        List<String> services = discoveryClient.getServices();
        log.info("从 Nacos 发现 {} 个服务: {}", services.size(), services);
        
        lastServicesHash.set(services.hashCode());
        
        // 过滤并按字母顺序排序
        List<String> sortedServices = services.stream()
                .filter(this::shouldIncludeService)
                .sorted()
                .toList();
        
        int registeredCount = 0;
        for (String serviceName : sortedServices) {
            registerService(serviceName);
            subscribeService(serviceName);
            registeredCount++;
        }
        
        log.info("自动发现完成，共注册 {} 个服务", registeredCount);
    }
    
    /**
     * 判断是否应该包含该服务
     */
    private boolean shouldIncludeService(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            return false;
        }
        
        return !properties.getDiscover().getExcludedServices().contains(serviceName);
    }
    
    /**
     * 注册单个服务
     */
    private synchronized void registerService(String serviceName) {
        if (registeredServices.contains(serviceName)) {
            return;
        }
        
        // 获取文档端点信息（优先使用探测服务，否则使用默认配置）
        DocEndpointInfo endpointInfo = getEndpointInfo(serviceName);
        String docPath = endpointInfo.getDocPath();
        String contextPath = properties.getDiscover().getContextPath(serviceName);
        
        // 缓存端点信息
        serviceEndpoints.put(serviceName, endpointInfo);
        
        // 构建路由配置
        Knife4jAggregatorProperties.Router router = new Knife4jAggregatorProperties.Router();
        router.setName(formatDisplayName(serviceName));
        router.setServiceName(serviceName);

        String effectivePrefix = (contextPath != null && !contextPath.isEmpty()) ? contextPath : ("/" + serviceName);
        router.setUrl(effectivePrefix + docPath);
        router.setOrder(UNAVAILABLE_SERVICE_BASE_ORDER + registeredServices.size());
        
        if (contextPath != null && !contextPath.isEmpty()) {
            router.setContextPath(contextPath);
        }

        // 添加到 routes
        properties.getRoutes().add(router);
        registeredServices.add(serviceName);
        serviceRouterMap.put(serviceName, router);
        
        // 添加到 gatewayResources (用于 OpenAPIEndpoint)
        OpenAPI2Resource resource = new OpenAPI2Resource(
                router.getUrl(),
                router.getOrder(),
                true,
                router.getName(),
                router.getContextPath() != null ? router.getContextPath() : "",
                serviceName
        );
        gatewayResources.add(resource);
        
        log.info("已注册服务: {} -> {}，contextPath: {}，OpenAPI版本: {}", 
                serviceName, router.getUrl(), router.getContextPath(), endpointInfo.getVersion());
    }
    
    /**
     * 获取服务的文档端点信息
     */
    private DocEndpointInfo getEndpointInfo(String serviceName) {
        if (endpointProbeService != null) {
            return endpointProbeService.getEndpointInfo(serviceName);
        }
        // 回退到默认配置
        return new DocEndpointInfo(
                properties.getDiscover().getDocPath(),
                properties.getDiscover().getVersion()
        );
    }
    
    /**
     * 获取服务的端点信息（供外部使用）
     */
    public DocEndpointInfo getServiceEndpointInfo(String serviceName) {
        DocEndpointInfo cached = serviceEndpoints.get(serviceName);
        if (cached != null) {
            return cached;
        }
        return getEndpointInfo(serviceName);
    }
    
    /**
     * 根据缓存可用性重新排序服务
     */
    private synchronized void reorderServicesByAvailability() {
        if (cacheManager == null) {
            return;
        }
        
        List<String> availableServices = new ArrayList<>();
        List<String> unavailableServices = new ArrayList<>();
        
        for (String serviceName : registeredServices) {
            if (cacheManager.hasCached(serviceName)) {
                availableServices.add(serviceName);
            } else {
                unavailableServices.add(serviceName);
            }
        }
        
        Collections.sort(availableServices);
        Collections.sort(unavailableServices);
        
        int order = AVAILABLE_SERVICE_BASE_ORDER;
        for (String serviceName : availableServices) {
            Knife4jAggregatorProperties.Router router = serviceRouterMap.get(serviceName);
            if (router != null) {
                router.setOrder(order++);
            }
        }
        
        order = UNAVAILABLE_SERVICE_BASE_ORDER;
        for (String serviceName : unavailableServices) {
            Knife4jAggregatorProperties.Router router = serviceRouterMap.get(serviceName);
            if (router != null) {
                router.setOrder(order++);
            }
        }
        
        // 重新排序 routes 列表
        properties.getRoutes().sort(Comparator.comparingInt(Knife4jAggregatorProperties.Router::getOrder));
        
        // 重建 gatewayResources
        rebuildGatewayResources();
        
        log.info("服务排序完成：可用服务 {} 个，不可用服务 {} 个", 
                availableServices.size(), unavailableServices.size());
    }
    
    /**
     * 重建 gatewayResources
     */
    private void rebuildGatewayResources() {
        gatewayResources.clear();
        for (Knife4jAggregatorProperties.Router router : properties.getRoutes()) {
            OpenAPI2Resource resource = new OpenAPI2Resource(
                    router.getUrl(),
                    router.getOrder(),
                    true,
                    router.getName(),
                    router.getContextPath() != null ? router.getContextPath() : "",
                    router.getServiceName()
            );
            gatewayResources.add(resource);
        }
    }
    
    /**
     * 更新单个服务为可用状态
     */
    private synchronized void markServiceAsAvailable(String serviceName) {
        Knife4jAggregatorProperties.Router router = serviceRouterMap.get(serviceName);
        if (router == null) {
            return;
        }
        
        if (router.getOrder() < UNAVAILABLE_SERVICE_BASE_ORDER) {
            return;
        }
        
        int maxAvailableOrder = AVAILABLE_SERVICE_BASE_ORDER - 1;
        for (Knife4jAggregatorProperties.Router r : properties.getRoutes()) {
            if (r.getOrder() < UNAVAILABLE_SERVICE_BASE_ORDER && r.getOrder() > maxAvailableOrder) {
                maxAvailableOrder = r.getOrder();
            }
        }
        
        router.setOrder(maxAvailableOrder + 1);
        
        properties.getRoutes().sort(Comparator.comparingInt(Knife4jAggregatorProperties.Router::getOrder));
        rebuildGatewayResources();
        
        log.info("服务 {} 缓存成功，order 更新为: {}", serviceName, router.getOrder());
    }
    
    /**
     * 格式化服务显示名称
     */
    private String formatDisplayName(String serviceName) {
        String format = properties.getDiscover().getDisplayNameFormat();
        if ("capitalize".equals(format)) {
            return capitalizeWords(serviceName.replace("-", " "));
        }
        return serviceName;
    }
    
    /**
     * 将字符串中每个单词的首字母大写
     */
    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 获取所有OpenAPI资源列表 (用于 OpenAPIEndpoint)
     */
    public List<OpenAPI2Resource> getResources(String forwardPath) {
        List<OpenAPI2Resource> resourceList = new ArrayList<>();
        
        for (OpenAPI2Resource resource : gatewayResources) {
            OpenAPI2Resource copy = resource.copy();
            copy.setContextPath(PathUtils.processContextPath(PathUtils.append(forwardPath, copy.getContextPath())));
            copy.setUrl(PathUtils.append(forwardPath, copy.getUrl()));
            resourceList.add(copy);
        }
        
        return resourceList;
    }
    
    /**
     * 获取已注册的服务列表
     */
    public Set<String> getRegisteredServices() {
        return new HashSet<>(registeredServices);
    }
    
    /**
     * 手动刷新服务发现
     */
    public void refreshServices() {
        log.info("手动刷新服务发现...");
        discoverAndRegisterServices();
        warmUpDocCache();
        reorderServicesByAvailability();
    }
    
    // ==================== 缓存集成方法 ====================
    
    /**
     * 预热文档缓存
     */
    private void warmUpDocCache() {
        if (cacheManager == null) {
            return;
        }
        
        cacheManager.warmUpCache(registeredServices);
    }
    
    /**
     * 刷新指定服务的文档缓存并更新排序
     */
    private void refreshServiceDocCacheAndReorder(String serviceName) {
        if (cacheManager == null) {
            return;
        }
        
        cacheManager.refreshServiceDocAsync(serviceName);
        
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                if (cacheManager.hasCached(serviceName)) {
                    markServiceAsAvailable(serviceName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "reorder-" + serviceName).start();
    }
    
    /**
     * 标记服务下线
     */
    private void markServiceOffline(String serviceName) {
        if (cacheManager == null) {
            return;
        }
        
        cacheManager.markServiceOffline(serviceName);
    }

}
