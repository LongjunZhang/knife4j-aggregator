package com.github.zhanglongjun.knife4j.ai.controller;

import com.github.zhanglongjun.knife4j.ai.dto.ExplainErrorRequest;
import com.github.zhanglongjun.knife4j.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 服务控制器
 * 
 * 提供流式错误解释 API (SSE)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    
    private final AiService aiService;
    
    /**
     * 流式解释接口错误 (SSE)
     * 
     * 真正的 Server-Sent Events 流式输出，像 ChatGPT 一样逐字返回。
     * 每个 chunk 作为一个 SSE event 推送给客户端。
     */
    @PostMapping(value = "/explain-error/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> explainErrorStream(
            @RequestBody ExplainErrorRequest request) {
        
        log.info("Received SSE explain-error request: service={}, path={}, method={}",
            request.getServiceName(), request.getPath(), request.getMethod());
        
        return aiService.explainErrorStream(request)
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build())
            .concatWith(Flux.just(
                ServerSentEvent.<String>builder()
                    .event("done")
                    .data("[DONE]")
                    .build()
            ))
            .onErrorResume(e -> {
                log.error("SSE stream error", e);
                return Flux.just(
                    ServerSentEvent.<String>builder()
                        .event("error")
                        .data("服务内部错误: " + e.getMessage())
                        .build()
                );
            });
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("OK"));
    }
}
