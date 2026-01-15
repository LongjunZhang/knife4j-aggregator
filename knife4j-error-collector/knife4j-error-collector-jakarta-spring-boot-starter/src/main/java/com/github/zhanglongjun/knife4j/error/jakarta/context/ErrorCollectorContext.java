package com.github.zhanglongjun.knife4j.error.jakarta.context;

/**
 * 错误收集器上下文
 * 使用 ThreadLocal 存储链路追踪信息
 */
public class ErrorCollectorContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();

    /**
     * 设置链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 获取链路追踪 ID
     */
    public static String getTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 设置 Span ID
     */
    public static void setSpanId(String spanId) {
        SPAN_ID.set(spanId);
    }

    /**
     * 获取 Span ID
     */
    public static String getSpanId() {
        return SPAN_ID.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
    }

}
