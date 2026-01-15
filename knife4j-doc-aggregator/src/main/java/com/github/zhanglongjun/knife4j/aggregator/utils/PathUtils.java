/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.utils;

import com.github.zhanglongjun.knife4j.aggregator.conf.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 路径工具类
 */
public class PathUtils {
    
    private static final Logger log = LoggerFactory.getLogger(PathUtils.class);
    
    static final String DOC_URL = "/doc.html";
    public static final String DEFAULT_CONTEXT_PATH = "/";
    
    static final Pattern PATTERN = Pattern.compile("(.*?)\\/doc\\.html", Pattern.CASE_INSENSITIVE);
    
    public static String getContextPath(String referer) {
        if (StringUtils.hasLength(referer)) {
            try {
                URI uri = URI.create(referer);
                String path = uri.getPath();
                Matcher mather = PATTERN.matcher(path);
                if (mather.find()) {
                    return mather.group(1);
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
        return "/";
    }
    
    public static String append(String... paths) {
        if (Objects.isNull(paths) || paths.length == 0) {
            return GlobalConstants.DEFAULT_API_PATH_PREFIX;
        }
        String fullPath = Arrays.stream(paths)
                .filter(StringUtils::hasLength)
                .map(path -> GlobalConstants.DEFAULT_API_PATH_PREFIX + path)
                .collect(Collectors.joining());
        return fullPath.replaceAll(GlobalConstants.DEFAULT_API_PATH_PREFIX + "+", GlobalConstants.DEFAULT_API_PATH_PREFIX);
    }
    
    /**
     * 获取默认请求ContextPath路径
     * @param request 当前请求对象实例
     * @return ContextPath路径
     */
    public static String getDefaultContextPath(ServerHttpRequest request) {
        String contextPath = request.getPath().contextPath().value();
        if (!StringUtils.hasLength(contextPath)) {
            List<String> referer = request.getHeaders().get("Referer");
            if (referer != null && !referer.isEmpty()) {
                String value = referer.get(0);
                log.debug("Referer:{}", value);
                contextPath = PathUtils.getContextPath(value);
            } else {
                contextPath = DEFAULT_CONTEXT_PATH;
            }
        }
        return contextPath;
    }
    
    /**
     * 数据校验，主要是针对ContextPath属性
     * @param contextPath 当前接口或服务ContextPath路径
     */
    public static String processContextPath(String contextPath) {
        String validateContextPath = contextPath;
        if (DEFAULT_CONTEXT_PATH.equals(validateContextPath)) {
            validateContextPath = "";
        }
        if (validateContextPath.endsWith(DEFAULT_CONTEXT_PATH)) {
            validateContextPath = validateContextPath.substring(0, validateContextPath.length() - 1);
        }
        return validateContextPath;
    }
    
    /**
     * 判断ContextPath非空,并且不等于"/"字符串
     * @param contextPath contextPath
     * @return True-空，FALSE-不满足
     */
    public static boolean contextPathNull(String contextPath) {
        return StrUtil.isNotBlank(contextPath) && !contextPath.equalsIgnoreCase(PathUtils.DEFAULT_CONTEXT_PATH);
    }
    
    private PathUtils() {
    }
}

