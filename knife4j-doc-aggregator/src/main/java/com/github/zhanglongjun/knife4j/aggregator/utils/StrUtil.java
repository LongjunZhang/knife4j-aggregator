/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.utils;

/**
 * 字符串工具类
 */
public class StrUtil {
    
    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\ufeff' || c == '\u202a';
    }
    
    public static boolean isBlank(CharSequence str) {
        int length;
        
        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }
        
        for (int i = 0; i < length; i++) {
            if (!isBlankChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }
    
    /**
     * 判断当前内容是否非空，如果是空，则用默认值替换
     * @param value 判断值
     * @param defaultStr 默认值
     * @return 非空判断值
     */
    public static String defaultTo(String value, String defaultStr) {
        if (isNotBlank(value)) {
            return value;
        } else {
            return defaultStr;
        }
    }
    
    private StrUtil() {
    }
}

