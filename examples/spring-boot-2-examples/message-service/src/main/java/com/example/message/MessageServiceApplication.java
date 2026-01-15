package com.example.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 消息服务启动类 (Spring Boot 2.7.x + springfox-boot-starter)
 * 使用 Swagger 2.0 注解，生成 /v2/api-docs 端点
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }

}

