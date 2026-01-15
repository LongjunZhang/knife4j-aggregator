package com.github.zhanglongjun.knife4j.error.jakarta.autoconfig;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.extractor.ErrorMetadataBuilder;
import com.github.zhanglongjun.knife4j.error.jakarta.extractor.BasePackageDetector;
import com.github.zhanglongjun.knife4j.error.extractor.StackExtractor;
import com.github.zhanglongjun.knife4j.error.jakarta.config.ErrorCollectorJakartaProperties;
import com.github.zhanglongjun.knife4j.error.jakarta.controller.ErrorDetailController;
import com.github.zhanglongjun.knife4j.error.jakarta.filter.JakartaErrorCollectorFilter;
import com.github.zhanglongjun.knife4j.error.jakarta.handler.GlobalExceptionHandler;
import com.github.zhanglongjun.knife4j.error.store.ErrorDetailStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Knife4j Error Collector 自动配置类 (Jakarta / Spring Boot 3.x)
 * 开箱即用模式：无需配置 Token，异常发生时自动收集
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "knife4j.error-collector.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ErrorCollectorJakartaProperties.class)
public class ErrorCollectorJakartaAutoConfiguration {

    private final ErrorCollectorJakartaProperties properties;
    private ErrorDetailStore errorDetailStore;

    public ErrorCollectorJakartaAutoConfiguration(ErrorCollectorJakartaProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("[ErrorCollector] ==========================================");
        log.info("[ErrorCollector] Knife4j Error Collector (Jakarta) 已启动");
        log.info("[ErrorCollector] 开箱即用模式：异常发生时自动收集错误信息");
        log.info("[ErrorCollector] 配置信息:");
        log.info("[ErrorCollector]   - 启用状态: {}", properties.isEnabled());
        log.info("[ErrorCollector]   - TTL: {} 分钟", properties.getTtlMinutes());
        log.info("[ErrorCollector]   - 最大存储错误数: {}", properties.getMaxStoredErrors());
        log.info("[ErrorCollector]   - 堆栈最大深度: {}", properties.getStackMaxDepth());
        log.info("[ErrorCollector]   - 堆栈片段最大帧数: {}", properties.getStackSnippetMaxFrames());
        log.info("[ErrorCollector]   - 包白名单: {}", properties.getPackageWhitelist());
        log.info("[ErrorCollector]   - 调试日志: {}", properties.isDebugLog());
        log.info("[ErrorCollector]   - 基础包名: {}", 
                properties.getBasePackage() != null ? properties.getBasePackage() : "(自动检测)");
        log.info("[ErrorCollector] ==========================================");
    }

    @PreDestroy
    public void destroy() {
        if (errorDetailStore != null) {
            errorDetailStore.stop();
        }
        log.info("[ErrorCollector] Knife4j Error Collector (Jakarta) 已停止");
    }

    @Bean
    @ConditionalOnMissingBean
    public StackExtractor stackExtractor() {
        return new StackExtractor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorMetadataBuilder errorMetadataBuilder(StackExtractor stackExtractor) {
        return new ErrorMetadataBuilder(properties, stackExtractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorDetailStore errorDetailStore() {
        this.errorDetailStore = new ErrorDetailStore(properties);
        errorDetailStore.start();
        return errorDetailStore;
    }

    @Bean
    @ConditionalOnMissingBean
    public BasePackageDetector basePackageDetector() {
        return new BasePackageDetector(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(
            ErrorMetadataBuilder metadataBuilder,
            ErrorDetailStore errorStore) {
        return new GlobalExceptionHandler(properties, metadataBuilder, errorStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorDetailController errorDetailController(ErrorDetailStore errorStore) {
        return new ErrorDetailController(properties, errorStore);
    }

    @Bean
    public FilterRegistrationBean<Filter> errorCollectorFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JakartaErrorCollectorFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("errorCollectorFilter");
        return registration;
    }

}
