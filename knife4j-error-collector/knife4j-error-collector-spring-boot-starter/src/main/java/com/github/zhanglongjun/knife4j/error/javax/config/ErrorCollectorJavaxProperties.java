package com.github.zhanglongjun.knife4j.error.javax.config;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot 2.x (Javax) 版本的配置属性
 */
@ConfigurationProperties(prefix = "knife4j.error-collector")
public class ErrorCollectorJavaxProperties extends ErrorCollectorProperties {

}

