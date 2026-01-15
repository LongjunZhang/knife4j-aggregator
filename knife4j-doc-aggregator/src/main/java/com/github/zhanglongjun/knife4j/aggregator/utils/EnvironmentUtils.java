/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.utils;

import org.springframework.core.env.Environment;

import java.util.Objects;

/**
 * 环境变量工具类
 */
public class EnvironmentUtils {
    
    /**
     * 获取String类型的配置值
     * @param environment Spring Context Environment
     * @param key 配置key
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String resolveString(Environment environment, String key, String defaultValue) {
        if (environment != null) {
            String envValue = environment.getProperty(key);
            if (StrUtil.isNotBlank(envValue)) {
                return envValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取int类型的值
     * @param environment 环境变量
     * @param key 变量
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static Integer resolveInt(Environment environment, String key, Integer defaultValue) {
        if (environment != null) {
            return Integer.parseInt(Objects.toString(
                    environment.getProperty(key, String.valueOf(defaultValue)), 
                    String.valueOf(defaultValue)));
        }
        return defaultValue;
    }
    
    /**
     * 获取bool值
     * @param environment 环境变量
     * @param key 变量
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static Boolean resolveBool(Environment environment, String key, Boolean defaultValue) {
        if (environment != null) {
            return Boolean.valueOf(Objects.toString(
                    environment.getProperty(key, defaultValue.toString()), 
                    defaultValue.toString()));
        }
        return defaultValue;
    }
    
    private EnvironmentUtils() {
    }
}

