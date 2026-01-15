/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.autoconfig;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.github.zhanglongjun.knife4j.aggregator.cache.ApiDocsCacheFilter;
import com.github.zhanglongjun.knife4j.aggregator.cache.ApiDocsCacheManager;
import com.github.zhanglongjun.knife4j.aggregator.conf.GlobalConstants;
import com.github.zhanglongjun.knife4j.aggregator.config.ErrorCollectorProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.filter.ErrorDetailInterceptorFilter;
import com.github.zhanglongjun.knife4j.aggregator.service.ErrorDetailCentralStore;
import com.github.zhanglongjun.knife4j.aggregator.controller.CacheManagementController;
import com.github.zhanglongjun.knife4j.aggregator.controller.OpenAPIEndpoint;
import com.github.zhanglongjun.knife4j.aggregator.controller.VersionController;
import com.github.zhanglongjun.knife4j.aggregator.filter.ApiVersionValidationFilter;
import com.github.zhanglongjun.knife4j.aggregator.filter.WebFluxSecurityBasicAuthFilter;
import com.github.zhanglongjun.knife4j.aggregator.service.ApiPathCacheService;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiChangeRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.ApiDocVersionRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.ServiceInfoRepository;
import com.github.zhanglongjun.knife4j.aggregator.repository.SyncLogRepository;
import com.github.zhanglongjun.knife4j.aggregator.service.AggregatorDiscoveryService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocDiffService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocEndpointProbeService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocPersistenceService;
import com.github.zhanglongjun.knife4j.aggregator.service.DocVersionService;
import com.github.zhanglongjun.knife4j.aggregator.service.Swagger2ToOAS3Converter;
import com.github.zhanglongjun.knife4j.aggregator.utils.EnvironmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import jakarta.annotation.PostConstruct;

/**
 * Knife4j 聚合器自动配置
 */
@Configuration
@EnableConfigurationProperties(Knife4jAggregatorProperties.class)
@ConditionalOnProperty(name = "knife4j.aggregator.enabled", havingValue = "true")
@EnableReactiveMongoRepositories(basePackages = "com.github.zhanglongjun.knife4j.aggregator.repository")
public class Knife4jAggregatorAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(Knife4jAggregatorAutoConfiguration.class);
    
    private final Environment environment;
    
    public Knife4jAggregatorAutoConfiguration(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * OpenAPI 端点
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPIEndpoint openAPIEndpoint(Knife4jAggregatorProperties properties) {
        return new OpenAPIEndpoint(properties);
    }
    
    /**
     * Swagger 2.0 到 OpenAPI 3.0 转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public Swagger2ToOAS3Converter swagger2ToOAS3Converter() {
        log.info("初始化 Swagger 2.0 到 OpenAPI 3.0 转换器");
        return new Swagger2ToOAS3Converter();
    }
    
    /**
     * 文档端点探测服务
     */
    @Bean
    @ConditionalOnMissingBean
    public DocEndpointProbeService docEndpointProbeService(
            Knife4jAggregatorProperties properties,
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder) {
        log.info("初始化文档端点探测服务");
        return new DocEndpointProbeService(properties, webClientBuilder);
    }
    
    /**
     * 服务发现配置
     */
    @Configuration
    @ConditionalOnProperty(name = "knife4j.aggregator.strategy", havingValue = "discover", matchIfMissing = true)
    public static class DiscoverConfiguration {
        
        /**
         * 聚合器服务发现服务
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = "knife4j.aggregator.discover.enabled", havingValue = "true", matchIfMissing = true)
        public AggregatorDiscoveryService aggregatorDiscoveryService(
                DiscoveryClient discoveryClient,
                Knife4jAggregatorProperties properties,
                NacosDiscoveryProperties nacosProperties,
                ApiDocsCacheManager cacheManager,
                DocEndpointProbeService endpointProbeService) {
            return new AggregatorDiscoveryService(
                    discoveryClient, properties, nacosProperties, 
                    cacheManager, endpointProbeService);
        }
    }
    
    /**
     * 缓存配置
     */
    @Configuration
    @ConditionalOnProperty(name = "knife4j.aggregator.cache.enabled", havingValue = "true", matchIfMissing = true)
    public static class CacheConfiguration {
        
        /**
         * API 文档缓存管理器
         */
        @Bean
        @ConditionalOnMissingBean
        public ApiDocsCacheManager apiDocsCacheManager(
                DiscoveryClient discoveryClient,
                Knife4jAggregatorProperties properties,
                Swagger2ToOAS3Converter converter) {
            return new ApiDocsCacheManager(discoveryClient, properties, converter);
        }
        
        /**
         * API 文档缓存过滤器
         */
        @Bean
        @ConditionalOnMissingBean
        public ApiDocsCacheFilter apiDocsCacheFilter(
                ApiDocsCacheManager cacheManager,
                Knife4jAggregatorProperties properties) {
            return new ApiDocsCacheFilter(cacheManager, properties);
        }
        
        /**
         * 缓存管理控制器
         */
        @Bean
        @ConditionalOnMissingBean
        public CacheManagementController cacheManagementController(
                ApiDocsCacheManager cacheManager,
                ApiDocVersionRepository versionRepository,
                ApiChangeRepository changeRepository,
                SyncLogRepository syncLogRepository,
                Knife4jAggregatorProperties properties) {
            return new CacheManagementController(cacheManager, versionRepository, 
                    changeRepository, syncLogRepository, properties);
        }
    }
    
    /**
     * MongoDB 持久化配置（M3 阶段）
     */
    @Configuration
    @ConditionalOnProperty(name = "spring.data.mongodb.uri")
    public static class MongoConfiguration {
        
        private static final Logger log = LoggerFactory.getLogger(MongoConfiguration.class);
        
        /**
         * 文档版本服务
         */
        @Bean
        @ConditionalOnMissingBean
        public DocVersionService docVersionService(
                ApiDocVersionRepository versionRepository,
                Swagger2ToOAS3Converter converter) {
            log.info("初始化文档版本服务");
            return new DocVersionService(versionRepository, converter);
        }
        
        /**
         * 文档对比服务
         */
        @Bean
        @ConditionalOnMissingBean
        public DocDiffService docDiffService() {
            log.info("初始化文档对比服务");
            return new DocDiffService();
        }
        
        /**
         * 文档持久化服务
         */
        @Bean
        @ConditionalOnMissingBean
        public DocPersistenceService docPersistenceService(
                DocVersionService versionService,
                DocDiffService diffService,
                ServiceInfoRepository serviceInfoRepository,
                ApiDocVersionRepository versionRepository,
                ApiChangeRepository changeRepository,
                SyncLogRepository syncLogRepository) {
            log.info("初始化文档持久化服务");
            return new DocPersistenceService(
                    versionService, diffService,
                    serviceInfoRepository, versionRepository,
                    changeRepository, syncLogRepository);
        }
        
        /**
         * 版本管理控制器
         */
        @Bean
        @ConditionalOnMissingBean
        public VersionController versionController(DocPersistenceService persistenceService,
                                                    Knife4jAggregatorProperties properties) {
            log.info("初始化版本管理控制器");
            return new VersionController(persistenceService, properties);
        }
        
        /**
         * API 路径缓存服务
         */
        @Bean
        @ConditionalOnMissingBean
        public ApiPathCacheService apiPathCacheService(DocVersionService versionService) {
            log.info("初始化 API 路径缓存服务");
            return new ApiPathCacheService(versionService);
        }
        
        /**
         * API 版本校验过滤器
         */
        @Bean
        @ConditionalOnMissingBean
        public ApiVersionValidationFilter apiVersionValidationFilter(
                ApiPathCacheService pathCacheService,
                Knife4jAggregatorProperties properties) {
            log.info("初始化 API 版本校验过滤器");
            return new ApiVersionValidationFilter(pathCacheService, properties);
        }
        
        /**
         * 持久化服务注入到缓存管理器
         */
        @Configuration
        @ConditionalOnBean({ApiDocsCacheManager.class, DocPersistenceService.class})
        public static class PersistenceInjector {
            
            @Autowired
            private ApiDocsCacheManager cacheManager;
            
            @Autowired
            private DocPersistenceService persistenceService;
            
            @Autowired(required = false)
            private DocEndpointProbeService endpointProbeService;
            
            @PostConstruct
            public void injectServices() {
                cacheManager.setPersistenceService(persistenceService);
                log.info("已将持久化服务注入到缓存管理器");
                
                if (endpointProbeService != null) {
                    cacheManager.setEndpointProbeService(endpointProbeService);
                    log.info("已将端点探测服务注入到缓存管理器");
                }
            }
        }
    }
    
    /**
     * Security with Basic Http
     */
    @Bean
    @ConditionalOnMissingBean(WebFluxSecurityBasicAuthFilter.class)
    @ConditionalOnProperty(name = "knife4j.aggregator.basic.enable", havingValue = "true")
    public WebFluxSecurityBasicAuthFilter securityBasicAuthFilter(Knife4jAggregatorProperties properties) {
        WebFluxSecurityBasicAuthFilter authFilter = new WebFluxSecurityBasicAuthFilter();
        if (properties == null || properties.getBasic() == null) {
            authFilter.setEnableBasicAuth(EnvironmentUtils.resolveBool(environment, "knife4j.aggregator.basic.enable", Boolean.FALSE));
            authFilter.setUserName(EnvironmentUtils.resolveString(environment, "knife4j.aggregator.basic.username", GlobalConstants.BASIC_DEFAULT_USERNAME));
            authFilter.setPassword(EnvironmentUtils.resolveString(environment, "knife4j.aggregator.basic.password", GlobalConstants.BASIC_DEFAULT_PASSWORD));
        } else {
            authFilter.setEnableBasicAuth(properties.getBasic().isEnable());
            authFilter.setUserName(properties.getBasic().getUsername());
            authFilter.setPassword(properties.getBasic().getPassword());
            authFilter.addRule(properties.getBasic().getInclude());
        }
        return authFilter;
    }
    
    /**
     * 错误详情拦截过滤器（Gateway GlobalFilter）
     * 
     * 拦截 Gateway 路由响应，处理 X-Error-Detail 响应头
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "knife4j.error-collector-proxy.enabled", havingValue = "true", matchIfMissing = true)
    public ErrorDetailInterceptorFilter errorDetailInterceptorFilter(
            ErrorCollectorProxyProperties errorCollectorProperties,
            ErrorDetailCentralStore centralStore) {
        log.info("初始化错误详情拦截过滤器");
        return new ErrorDetailInterceptorFilter(errorCollectorProperties, centralStore);
    }
}
