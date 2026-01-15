package com.github.zhanglongjun.knife4j.error.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Error Collector 配置属性
 * 配置前缀: knife4j.error-collector
 */
@Data
public class ErrorCollectorProperties {

    /**
     * 是否启用错误收集器，默认开启（开箱即用）
     */
    private boolean enabled = true;

    /**
     * 响应头名称，用于返回 errorId
     * 微服务通过此响应头返回 errorId，不在响应体中暴露错误详情
     */
    private String errorIdHeaderName = "X-Error-Id";

    /**
     * 异常链最大层数
     */
    private int stackMaxDepth = 5;

    /**
     * 堆栈片段最大帧数
     */
    private int stackSnippetMaxFrames = 10;

    /**
     * 堆栈片段最大字符数
     */
    private int stackSnippetMaxLength = 2048;

    /**
     * 包名白名单，只保留这些包的栈帧
     * 支持前缀匹配，如 "com.example" 会匹配 "com.example.user.service"
     */
    private List<String> packageWhitelist = new ArrayList<>();

    /**
     * 错误详情 TTL（分钟）
     */
    private int ttlMinutes = 30;

    /**
     * 过期清理间隔（分钟）
     */
    private int cleanupIntervalMinutes = 5;

    /**
     * 脱敏正则表达式列表
     * 匹配的内容会被替换为 [MASKED]
     */
    private List<String> maskPatterns = new ArrayList<>(Arrays.asList(
            // 密码、token、密钥等
            "(?i)(password|passwd|pwd|token|secret|api[_-]?key|access[_-]?key|authorization)\\s*[=:]\\s*[^\\s&,;\"']+",
            // 手机号（中国大陆）
            "1[3-9]\\d{9}",
            // 身份证号
            "\\d{17}[\\dXx]",
            // 邮箱
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            // 银行卡号（简化）
            "\\d{16,19}"
    ));

    /**
     * 脱敏替换文本
     */
    private String maskReplacement = "[MASKED]";

    /**
     * 是否在响应中包含 stackSnippet
     */
    private boolean includeStackSnippet = true;

    /**
     * 内部接口路径前缀
     */
    private String internalApiPrefix = "/internal/error-collector";

    /**
     * 最大存储的错误数量，防止内存溢出
     */
    private int maxStoredErrors = 10000;

    /**
     * 应用基础包名（可选，不配置则自动检测）
     * 用于识别业务代码栈帧
     */
    private String basePackage;

    /**
     * 是否启用详细调试日志
     * 开启后会输出错误收集过程的详细日志
     */
    private boolean debugLog = true;

    // ===================== 响应头传输模式配置 =====================

    /**
     * 是否启用响应头传输模式
     * 启用后，ErrorDetail 会编码后放入响应头，由 doc-aggregator 代理层拦截并存储
     * 业务服务本地不再存储 ErrorDetail
     */
    private boolean headerTransferEnabled = true;

    /**
     * 响应头名称：携带编码后的错误详情
     */
    private String errorDetailHeaderName = "X-Error-Detail";

    /**
     * 错误详情响应头的最大长度（字节）
     * 超过此长度会截断 stackTrace
     */
    private int errorDetailHeaderMaxLength = 8192;

}


