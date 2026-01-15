package com.github.zhanglongjun.knife4j.aggregator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * AI 代理配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "knife4j.ai")
public class AiProxyProperties {
    
    /**
     * 是否启用 AI 功能
     */
    private boolean enabled = true;
    
    /**
     * AI 服务地址
     */
    private String serviceUrl = "http://localhost:9100";
    
    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(60);
    
    /**
     * 安全配置
     */
    private SafetyConfig safety = new SafetyConfig();
    
    /**
     * 业务响应判断配置
     */
    private BusinessResponseConfig businessResponse = new BusinessResponseConfig();
    
    @Data
    public static class SafetyConfig {
        /**
         * 需要脱敏的 header 名称列表（小写）
         */
        private List<String> redactHeaders = Arrays.asList(
            "authorization", "cookie", "set-cookie"
        );
    }
    
    @Data
    public static class BusinessResponseConfig {
        /**
         * 是否启用业务响应判断
         */
        private boolean enabled = true;
        
        /**
         * 成功状态码字段名（如 code, status）
         */
        private String successField = "code";
        
        /**
         * 成功状态码值（如 2000, 1000, 200）
         */
        private String successValue = "2000";
    }
}





