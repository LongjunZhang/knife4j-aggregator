/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义化版本号
 * 
 * 格式: major.minor.patch (例如: 1.0.0, 1.2.3)
 * 
 * 版本升级规则:
 * - MAJOR: 有 Tag（模块）增删时，major+1，minor和patch重置为0
 * - MINOR: 有接口（Path）增删时，minor+1，patch重置为0
 * - PATCH: 接口内容修改时（参数、响应等），patch+1
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
    
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    
    private final int major;
    private final int minor;
    private final int patch;
    
    /**
     * 变更级别枚举
     */
    public enum ChangeLevel {
        MAJOR,  // Tag 增删
        MINOR,  // 接口增删
        PATCH,  // 接口内容修改
        NONE    // 无变更
    }
    
    public SemanticVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("版本号不能为负数");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    /**
     * 从字符串解析版本号
     * 
     * @param version 版本字符串，如 "1.0.0"
     * @return SemanticVersion 对象
     */
    public static SemanticVersion parse(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的语义化版本号格式: " + version + "，应为 x.y.z 格式");
        }
        
        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }
    
    /**
     * 尝试从字符串解析版本号，如果失败则返回 null
     */
    public static SemanticVersion tryParse(String version) {
        try {
            return parse(version);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从旧的整数版本号迁移
     * 
     * @param intVersion 整数版本号 (1, 2, 3...)
     * @return 语义化版本号 (1.0.0, 2.0.0, 3.0.0...)
     */
    public static SemanticVersion fromIntVersion(int intVersion) {
        return new SemanticVersion(intVersion, 0, 0);
    }
    
    /**
     * 初始版本
     */
    public static SemanticVersion initial() {
        return new SemanticVersion(1, 0, 0);
    }
    
    /**
     * 根据变更级别升级版本
     * 
     * @param changeLevel 变更级别
     * @return 新版本
     */
    public SemanticVersion bump(ChangeLevel changeLevel) {
        switch (changeLevel) {
            case MAJOR:
                return bumpMajor();
            case MINOR:
                return bumpMinor();
            case PATCH:
                return bumpPatch();
            case NONE:
            default:
                return this;
        }
    }
    
    /**
     * 升级主版本号
     * major+1, minor和patch重置为0
     */
    public SemanticVersion bumpMajor() {
        return new SemanticVersion(major + 1, 0, 0);
    }
    
    /**
     * 升级次版本号
     * minor+1, patch重置为0
     */
    public SemanticVersion bumpMinor() {
        return new SemanticVersion(major, minor + 1, 0);
    }
    
    /**
     * 升级修订号
     * patch+1
     */
    public SemanticVersion bumpPatch() {
        return new SemanticVersion(major, minor, patch + 1);
    }
    
    public int getMajor() {
        return major;
    }
    
    public int getMinor() {
        return minor;
    }
    
    public int getPatch() {
        return patch;
    }
    
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
    
    @Override
    public int compareTo(SemanticVersion other) {
        if (other == null) {
            return 1;
        }
        
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        
        int minorCompare = Integer.compare(this.minor, other.minor);
        if (minorCompare != 0) {
            return minorCompare;
        }
        
        return Integer.compare(this.patch, other.patch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SemanticVersion that = (SemanticVersion) obj;
        return major == that.major && minor == that.minor && patch == that.patch;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}

