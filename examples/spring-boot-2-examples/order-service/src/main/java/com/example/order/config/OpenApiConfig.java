package com.example.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 配置类 (Spring Boot 2.7.x)
 * 使用 SpringDoc OpenAPI UI
 */
@Configuration
public class OpenApiConfig {

    /**
     * 服务的 context-path，用于设置 OpenAPI 的 server URL
     * 这样通过 Gateway 访问时，Knife4j UI 会自动在请求路径前加上这个前缀
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("订单服务 API")
                        .description("订单服务接口文档 (Spring Boot 2.7.x + springdoc-openapi-ui)")
                        .version("1.0"))
                .servers(List.of(
                        new Server()
                                .url(contextPath)
                                .description("Gateway 代理路径")
                ));
    }

}

