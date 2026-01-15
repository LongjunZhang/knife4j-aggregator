package com.github.zhanglongjun.knife4j.error.javax.extractor;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 应用基础包名自动检测器
 * 
 * 检测策略（按优先级）：
 * 1. 使用配置属性 knife4j.error-collector.base-package
 * 2. 使用 packageWhitelist 的第一个包名
 * 3. 自动检测 @SpringBootApplication 注解类所在的包
 */
@Slf4j
public class BasePackageDetector implements ApplicationContextAware {

    private final ErrorCollectorProperties properties;
    private String detectedBasePackage;
    private ApplicationContext applicationContext;

    public BasePackageDetector(ErrorCollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        detectBasePackage();
    }

    /**
     * 检测应用基础包名
     */
    private void detectBasePackage() {
        // 策略1：使用配置的 basePackage
        if (StringUtils.hasText(properties.getBasePackage())) {
            this.detectedBasePackage = properties.getBasePackage();
            log.info("[ErrorCollector] 使用配置的基础包名: {}", detectedBasePackage);
            return;
        }

        // 策略2：使用 packageWhitelist 的第一个包名
        if (properties.getPackageWhitelist() != null && !properties.getPackageWhitelist().isEmpty()) {
            this.detectedBasePackage = properties.getPackageWhitelist().get(0);
            log.info("[ErrorCollector] 使用包白名单第一项作为基础包名: {}", detectedBasePackage);
            return;
        }

        // 策略3：自动检测 @SpringBootApplication 注解类所在的包
        try {
            Map<String, Object> candidates = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
            if (!candidates.isEmpty()) {
                Object mainBean = candidates.values().iterator().next();
                Class<?> mainClass = mainBean.getClass();
                
                // 处理 CGLIB 代理类
                if (mainClass.getName().contains("$$")) {
                    mainClass = mainClass.getSuperclass();
                }
                
                String packageName = mainClass.getPackage().getName();
                this.detectedBasePackage = packageName;
                log.info("[ErrorCollector] 自动检测到基础包名: {} (来自 {})", detectedBasePackage, mainClass.getSimpleName());
                return;
            }
        } catch (Exception e) {
            log.warn("[ErrorCollector] 自动检测基础包名失败: {}", e.getMessage());
        }

        // 兜底：使用默认值
        this.detectedBasePackage = "com";
        log.warn("[ErrorCollector] 无法检测基础包名，使用默认值: {}", detectedBasePackage);
    }

    /**
     * 获取检测到的基础包名
     */
    public String getBasePackage() {
        return detectedBasePackage;
    }

    /**
     * 检查类名是否属于应用的基础包
     */
    public boolean isApplicationPackage(String className) {
        if (className == null || detectedBasePackage == null) {
            return false;
        }
        return className.startsWith(detectedBasePackage);
    }

    /**
     * 从堆栈中查找第一个属于应用包的栈帧位置
     */
    public String findFirstApplicationFrame(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        for (StackTraceElement element : stackTrace) {
            if (isApplicationPackage(element.getClassName())) {
                return formatStackTraceElement(element);
            }
        }
        return null;
    }

    /**
     * 格式化栈帧为可读字符串
     */
    private String formatStackTraceElement(StackTraceElement element) {
        return String.format("%s.%s(%s:%d)",
                element.getClassName(),
                element.getMethodName(),
                element.getFileName() != null ? element.getFileName() : "Unknown",
                element.getLineNumber());
    }
}

