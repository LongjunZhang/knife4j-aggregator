package com.github.zhanglongjun.knife4j.aggregator.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.aggregator.config.ErrorCollectorProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.service.ErrorDetailCentralStore;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 错误详情拦截过滤器（Gateway GlobalFilter）
 * 
 * 拦截 Gateway 路由响应，处理 X-Error-Detail 响应头：
 * 1. 解码 X-Error-Detail（Base64 -> GZIP -> JSON）
 * 2. 存储错误详情到 ErrorDetailCentralStore
 * 3. 移除 X-Error-Detail 响应头（敏感信息不暴露给前端）
 * 4. 保留 X-Error-Id 响应头
 * 5. 添加 Access-Control-Expose-Headers 以便前端 JS 能读取 X-Error-Id
 */
@Slf4j
public class ErrorDetailInterceptorFilter implements GlobalFilter, Ordered {

    private final ErrorCollectorProxyProperties properties;
    private final ErrorDetailCentralStore centralStore;
    private final ObjectMapper objectMapper;

    public ErrorDetailInterceptorFilter(
            ErrorCollectorProxyProperties properties,
            ErrorDetailCentralStore centralStore) {
        this.properties = properties;
        this.centralStore = centralStore;
        this.objectMapper = new ObjectMapper();
        log.info("ErrorDetailInterceptorFilter initialized");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String requestPath = exchange.getRequest().getPath().value();
        log.debug("[ErrorDetailInterceptorFilter] Processing request: {}", requestPath);

        ServerHttpResponse originalResponse = exchange.getResponse();

        // 使用装饰器包装响应，以便在响应写入前处理响应头
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                log.debug("[ErrorDetailInterceptorFilter] writeWith called for: {}, status: {}", 
                        requestPath, getDelegate().getStatusCode());
                // 在响应写入前处理响应头
                processErrorDetailHeader(getDelegate());
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                log.debug("[ErrorDetailInterceptorFilter] writeAndFlushWith called for: {}", requestPath);
                // 在响应写入前处理响应头
                processErrorDetailHeader(getDelegate());
                return super.writeAndFlushWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 处理错误详情响应头
     */
    private void processErrorDetailHeader(ServerHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        
        String errorDetailHeaderName = properties.getErrorDetailHeaderName();
        String errorIdHeaderName = properties.getErrorIdHeaderName();
        
        String errorDetail = headers.getFirst(errorDetailHeaderName);
        String errorId = headers.getFirst(errorIdHeaderName);
        
        if (errorDetail != null && errorId != null) {
            log.debug("Intercepted error detail header: errorId={}", errorId);
            
            // 解码并存储错误详情
            try {
                Map<String, Object> detail = decodeErrorDetail(errorDetail);
                if (detail != null) {
                    // 打印原始错误详情，便于观察业务服务的错误内容
                    log.info("Received error detail from service: errorId={}, detail={}", errorId, detail);
                    centralStore.store(errorId, detail);
                    log.info("Stored error detail from Gateway response: errorId={}, storeSize={}", 
                            errorId, centralStore.size());
                }
            } catch (Exception e) {
                log.error("Failed to decode/store error detail: errorId={}", errorId, e);
            }
            
            // 移除 X-Error-Detail 响应头（敏感信息不暴露给前端）
            headers.remove(errorDetailHeaderName);
            log.debug("Removed {} header from response", errorDetailHeaderName);
        }
        
        // 如果有 X-Error-Id，确保前端能读取
        if (errorId != null) {
            // 添加或更新 Access-Control-Expose-Headers
            List<String> exposeHeaders = headers.getAccessControlExposeHeaders();
            if (!exposeHeaders.contains(errorIdHeaderName)) {
                headers.addIfAbsent("Access-Control-Expose-Headers", errorIdHeaderName);
            }
        }
    }

    /**
     * 解码错误详情
     * 格式：Base64 -> GZIP 解压 -> JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeErrorDetail(String encoded) {
        try {
            // Base64 解码
            byte[] compressed = Base64.getDecoder().decode(encoded);
            
            // GZIP 解压
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPInputStream gzipIs = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipIs.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            String json = baos.toString(StandardCharsets.UTF_8);
            
            // JSON 解析
            return objectMapper.readValue(json, Map.class);
            
        } catch (Exception e) {
            log.error("Failed to decode error detail", e);
            return null;
        }
    }

    @Override
    public int getOrder() {
        // 使用高优先级，确保装饰器在外层，能及时处理响应头
        // NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER = -1
        // 设置为 -2，比 NettyWriteResponseFilter 更早创建装饰器，装饰器更外层
        return -2;
    }
}
