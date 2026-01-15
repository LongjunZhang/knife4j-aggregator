package com.example.message.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Springfox Swagger 2 配置类
 * 生成 Swagger 2.0 规范的 /v2/api-docs 端点
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket messageApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.message.controller"))
                .paths(PathSelectors.any())
                .build();
        // 注意：不要设置 pathMapping(contextPath)，因为 context-path 已由 Spring 自动处理
        // 否则会导致路径重复，如 /messageService/messageService/message
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("消息服务 API")
                .description("消息服务接口文档 (Spring Boot 2.7.x + Springfox Swagger 2.0)")
                .version("1.0.0")
                .contact(new Contact("开发团队", "", "dev@example.com"))
                .build();
    }

    /**
     * 修复 Springfox 与 Spring Boot 2.6+ / Actuator 的兼容性问题
     * 过滤掉使用 PathPatternParser 的 HandlerMapping（如 Actuator 端点）
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                // 过滤掉使用 PathPatternParser 的 mapping（这些 mapping 的 getPatternParser() 不为 null）
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}

