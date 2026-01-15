package com.github.zhanglongjun.knife4j.error.javax.filter;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.javax.context.ErrorCollectorContext;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Javax Servlet Filter
 * 拦截请求，提取链路追踪信息
 */
@Slf4j
public class JavaxErrorCollectorFilter implements Filter {

    private final ErrorCollectorProperties properties;

    public JavaxErrorCollectorFilter(ErrorCollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[ErrorCollector] JavaxErrorCollectorFilter 已初始化（开箱即用模式）");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;

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
        log.info("[ErrorCollector] JavaxErrorCollectorFilter 已销毁");
    }

}
