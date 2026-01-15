package com.github.zhanglongjun.knife4j.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * AI 服务配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "knife4j.ai")
public class AiProperties {
    
    /**
     * 当前使用的 provider
     */
    private String provider = "ollama";
    
    /**
     * Ollama 配置
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    /**
     * 超时配置
     */
    private TimeoutConfig timeouts = new TimeoutConfig();
    
    /**
     * 安全配置
     */
    private SafetyConfig safety = new SafetyConfig();
    
    /**
     * 输出配置
     */
    private OutputConfig output = new OutputConfig();
    
    @Data
    public static class OllamaConfig {
        /**
         * Ollama 服务地址
         */
        private String baseUrl = "http://localhost:11434";
        
        /**
         * 使用的模型
         */
        private String model = "qwen2.5:3b-instruct";
    }
    
    @Data
    public static class TimeoutConfig {
        /**
         * 聊天超时时间
         */
        private Duration chat = Duration.ofSeconds(60);
    }
    
    @Data
    public static class SafetyConfig {
        /**
         * 需要脱敏的 header 名称列表
         */
        private List<String> redactHeaders = Arrays.asList(
            "authorization", "cookie", "set-cookie"
        );
    }
    
    @Data
    public static class OutputConfig {
        /**
         * JSON 解析失败时的最大重试次数
         */
        private int maxRetries = 2;
    }
}





