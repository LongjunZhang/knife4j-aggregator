package com.github.zhanglongjun.knife4j.error.extractor;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.model.CauseChainItem;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 堆栈信息提取器
 * 负责：提取异常链、过滤栈帧、脱敏信息、生成堆栈指纹
 */
@Slf4j
public class StackExtractor {

    private final ErrorCollectorProperties properties;
    private final List<Pattern> maskPatterns;

    public StackExtractor(ErrorCollectorProperties properties) {
        this.properties = properties;
        this.maskPatterns = compileMaskPatterns(properties.getMaskPatterns());
    }

    private List<Pattern> compileMaskPatterns(List<String> patterns) {
        List<Pattern> compiled = new ArrayList<>();
        if (patterns != null) {
            for (String pattern : patterns) {
                try {
                    compiled.add(Pattern.compile(pattern));
                } catch (Exception e) {
                    log.warn("Invalid mask pattern: {}", pattern, e);
                }
            }
        }
        return compiled;
    }

    /**
     * 提取异常链
     *
     * @param throwable 顶层异常
     * @return 异常链列表
     */
    public List<CauseChainItem> extractCauseChain(Throwable throwable) {
        List<CauseChainItem> chain = new ArrayList<>();
        Throwable current = throwable;
        int level = 0;
        int maxDepth = properties.getStackMaxDepth();

        while (current != null && level < maxDepth) {
            CauseChainItem item = CauseChainItem.builder()
                    .exceptionClass(current.getClass().getName())
                    .message(maskSensitiveData(current.getMessage()))
                    .level(level)
                    .location(extractLocation(current))
                    .build();
            chain.add(item);

            current = current.getCause();
            level++;
        }

        return chain;
    }

    /**
     * 提取异常发生的代码位置
     */
    private String extractLocation(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "unknown";
        }

        // 尝试找到业务包的第一个栈帧
        for (StackTraceElement element : stackTrace) {
            if (isBusinessPackage(element.getClassName())) {
                return formatStackTraceElement(element);
            }
        }

        // 如果没有找到业务包，返回第一个栈帧
        return formatStackTraceElement(stackTrace[0]);
    }

    private String formatStackTraceElement(StackTraceElement element) {
        return String.format("%s.%s:%d",
                element.getClassName(),
                element.getMethodName(),
                element.getLineNumber());
    }

    /**
     * 获取根因异常
     */
    public Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * 获取完整堆栈跟踪字符串
     */
    public String getFullStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取过滤后的堆栈跟踪（只包含业务包）
     */
    public String getFilteredStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        int maxDepth = properties.getStackMaxDepth();

        while (current != null && depth < maxDepth) {
            if (depth > 0) {
                sb.append("\nCaused by: ");
            }
            sb.append(current.getClass().getName())
                    .append(": ")
                    .append(maskSensitiveData(current.getMessage()))
                    .append("\n");

            StackTraceElement[] stackTrace = current.getStackTrace();
            int frameCount = 0;
            int maxFrames = properties.getStackSnippetMaxFrames();

            for (StackTraceElement element : stackTrace) {
                if (isBusinessPackage(element.getClassName())) {
                    sb.append("\tat ")
                            .append(element.toString())
                            .append("\n");
                    frameCount++;
                    if (frameCount >= maxFrames) {
                        break;
                    }
                }
            }

            current = current.getCause();
            depth++;
        }

        String result = sb.toString();
        int maxLength = properties.getStackSnippetMaxLength();
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength) + "\n... (truncated)";
        }

        return result;
    }

    /**
     * 生成堆栈片段（用于返回给调用方）
     */
    public String generateStackSnippet(Throwable throwable) {
        if (!properties.isIncludeStackSnippet()) {
            return null;
        }
        return getFilteredStackTrace(throwable);
    }

    /**
     * 生成堆栈指纹（用于聚合相同类型的错误）
     */
    public String generateStackFingerprint(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;

        while (current != null && depth < 3) {
            sb.append(current.getClass().getName()).append("|");

            // 取前 3 个业务包的栈帧
            StackTraceElement[] stackTrace = current.getStackTrace();
            int frameCount = 0;
            for (StackTraceElement element : stackTrace) {
                if (isBusinessPackage(element.getClassName())) {
                    sb.append(element.getClassName())
                            .append(".")
                            .append(element.getMethodName())
                            .append(":")
                            .append(element.getLineNumber())
                            .append("|");
                    frameCount++;
                    if (frameCount >= 3) {
                        break;
                    }
                }
            }

            current = current.getCause();
            depth++;
        }

        return md5Hash(sb.toString());
    }

    /**
     * 检查类名是否属于业务包
     */
    private boolean isBusinessPackage(String className) {
        List<String> whitelist = properties.getPackageWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            // 如果没有配置白名单，排除常见的框架包
            return !isFrameworkPackage(className);
        }

        for (String prefix : whitelist) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否是框架包（排除）
     */
    private boolean isFrameworkPackage(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("org.springframework.")
                || className.startsWith("org.apache.")
                || className.startsWith("org.hibernate.")
                || className.startsWith("org.mybatis.")
                || className.startsWith("com.zaxxer.")
                || className.startsWith("io.netty.")
                || className.startsWith("reactor.")
                || className.startsWith("org.slf4j.")
                || className.startsWith("ch.qos.logback.")
                || className.startsWith("org.aspectj.")
                || className.startsWith("cglib.")
                || className.startsWith("net.sf.cglib.")
                || className.contains("$$");
    }

    /**
     * 脱敏敏感数据
     */
    public String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Pattern pattern : maskPatterns) {
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll(properties.getMaskReplacement());
        }

        return result;
    }

    /**
     * 计算 MD5 哈希
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 不应该发生
            return String.valueOf(input.hashCode());
        }
    }

}

