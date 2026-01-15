package com.github.zhanglongjun.knife4j.error.jakarta.filter;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.jakarta.context.ErrorCollectorContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Jakarta Servlet Filter
 * 拦截请求，提取链路追踪信息
 */
@Slf4j
public class JakartaErrorCollectorFilter implements Filter {

    private final ErrorCollectorProperties properties;

    public JakartaErrorCollectorFilter(ErrorCollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[ErrorCollector] JakartaErrorCollectorFilter 已初始化（开箱即用模式）");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // 尝试获取链路追踪信息
                String traceId = httpRequest.getHeader("X-Trace-Id");
                if (traceId == null) {
                    traceId = httpRequest.getHeader("X-Request-Id");
                }
                if (traceId != null) {
                    ErrorCollectorContext.setTraceId(traceId);
                }

                String spanId = httpRequest.getHeader("X-Span-Id");
                if (spanId != null) {
                    ErrorCollectorContext.setSpanId(spanId);
                }

                if (properties.isDebugLog() && (traceId != null || spanId != null)) {
                    log.debug("[ErrorCollector] 链路追踪信息已设置 | TraceId: {} | SpanId: {}", 
                            traceId, spanId);
                }
            }

            chain.doFilter(request, response);

        } finally {
            ErrorCollectorContext.clear();
        }
    }

    @Override
    public void destroy() {
        log.info("[ErrorCollector] JakartaErrorCollectorFilter 已销毁");
    }

}
