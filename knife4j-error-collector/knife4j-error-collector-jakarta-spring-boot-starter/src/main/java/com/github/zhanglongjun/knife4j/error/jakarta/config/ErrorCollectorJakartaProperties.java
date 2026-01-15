package com.github.zhanglongjun.knife4j.error.jakarta.config;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot 3.x (Jakarta) 版本的配置属性
 */
@ConfigurationProperties(prefix = "knife4j.error-collector")
public class ErrorCollectorJakartaProperties extends ErrorCollectorProperties {

}

