/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 静态资源配置
 * 
 * Spring Cloud Gateway 基于 WebFlux，需要显式配置静态资源路由，
 * 否则静态资源请求可能会被 Gateway 路由机制拦截。
 */
@Configuration
public class StaticResourceConfig {

    /**
     * 配置静态资源路由
     * 优先级高于 Gateway 路由，确保静态资源正确处理
     */
    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources("/webjars/**", new ClassPathResource("static/webjars/"))
                .and(RouterFunctions.resources("/img/**", new ClassPathResource("static/img/")))
                .and(RouterFunctions.resources("/oauth/**", new ClassPathResource("static/oauth/")))
                .and(RouterFunctions.resources("/samples/**", new ClassPathResource("static/samples/")))
                .and(RouterFunctions.route()
                        .GET("/doc.html", request -> 
                            ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .bodyValue(new ClassPathResource("static/doc.html")))
                        .GET("/favicon.ico", request -> 
                            ServerResponse.ok()
                                .contentType(MediaType.valueOf("image/x-icon"))
                                .bodyValue(new ClassPathResource("static/favicon.ico")))
                        .GET("/robots.txt", request -> 
                            ServerResponse.ok()
                                .contentType(MediaType.TEXT_PLAIN)
                                .bodyValue(new ClassPathResource("static/robots.txt")))
                        .build());
    }
}
