/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 抽象basic认证过滤器
 */
public abstract class AbstractBasicAuthFilter {
    
    private static final Logger log = LoggerFactory.getLogger(AbstractBasicAuthFilter.class);
    
    public static final String BASIC = "Basic";
    
    protected List<Pattern> urlFilters = null;
    
    protected AbstractBasicAuthFilter() {
        urlFilters = new ArrayList<>();
        urlFilters.add(Pattern.compile(".*?/doc\\.html.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/v2/api-docs.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/v2/api-docs-ext.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/swagger-resources.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/swagger-resources/configuration/ui.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/swagger-resources/configuration/security.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/swagger-ui.*", Pattern.CASE_INSENSITIVE));
        urlFilters.add(Pattern.compile(".*?/v3/api-docs.*", Pattern.CASE_INSENSITIVE));
    }
    
    public List<Pattern> getUrlFilters() {
        return urlFilters;
    }
    
    /**
     * 添加外部过滤规则
     */
    public void addRule(String rule) {
        this.urlFilters.add(Pattern.compile(rule, Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * 添加外部过滤规则
     */
    public void addRule(Collection<String> rules) {
        if (rules != null && !rules.isEmpty()) {
            rules.forEach(this::addRule);
        }
    }
    
    /**
     * 判断是否匹配
     */
    protected boolean match(String uri) {
        if (uri != null) {
            String newUri = uri.replaceAll("/+", "/");
            for (Pattern pattern : getUrlFilters()) {
                if (pattern.matcher(newUri).matches()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * base64解码
     */
    protected String decodeBase64(String source) {
        String decodeStr = null;
        if (source != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(source);
                decodeStr = new String(bytes);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return decodeStr;
    }
}

