package com.github.zhanglongjun.knife4j.aggregator.controller;

import com.github.zhanglongjun.knife4j.aggregator.model.AnalyzeJob;
import com.github.zhanglongjun.knife4j.aggregator.model.RequestContext;
import com.github.zhanglongjun.knife4j.aggregator.service.ErrorAnalyzeService;
import com.github.zhanglongjun.knife4j.aggregator.service.ErrorCollectorProxyService;
import com.github.zhanglongjun.knife4j.aggregator.service.RequestContextCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 错误分析控制器
 * 提供错误分析任务管理和 SSE 结果推送
 */
@Slf4j
@RestController
@RequestMapping("/api/error-analyze")
@ConditionalOnProperty(name = "knife4j.error-collector-proxy.enabled", havingValue = "true", matchIfMissing = true)
public class ErrorAnalyzeController {

    private final ErrorAnalyzeService analyzeService;
    private final RequestContextCacheService requestContextCache;
    private final ErrorCollectorProxyService errorCollectorProxy;

    public ErrorAnalyzeController(
            ErrorAnalyzeService analyzeService,
            RequestContextCacheService requestContextCache,
            ErrorCollectorProxyService errorCollectorProxy) {
        this.analyzeService = analyzeService;
        this.requestContextCache = requestContextCache;
        this.errorCollectorProxy = errorCollectorProxy;
    }

    /**
     * 提交错误分析任务
     *
     * @param request 包含 requestId 和/或 errorId
     * @return 任务信息
     */
    @PostMapping("/submit")
    public Mono<ResponseEntity<Map<String, Object>>> submitAnalyze(@RequestBody Map<String, String> request) {
        String requestId = request.get("requestId");
        String errorId = request.get("errorId");

        if ((requestId == null || requestId.isEmpty()) && (errorId == null || errorId.isEmpty())) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "requestId or errorId is required"
            )));
        }

        return analyzeService.submitAnalyzeJob(requestId, errorId)
                .map(job -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("jobId", job.getJobId());
                    response.put("status", job.getStatus().name());
                    response.put("createdAt", job.getCreatedAt().toString());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Failed to submit analyze job", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                            "error", "SUBMIT_FAILED",
                            "message", e.getMessage()
                    )));
                });
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        Optional<AnalyzeJob> jobOpt = analyzeService.getJob(jobId);

        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalyzeJob job = jobOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("status", job.getStatus().name());
        response.put("errorId", job.getErrorId());
        response.put("serviceName", job.getServiceName());
        response.put("createdAt", job.getCreatedAt().toString());

        if (job.getStartedAt() != null) {
            response.put("startedAt", job.getStartedAt().toString());
        }
        if (job.getCompletedAt() != null) {
            response.put("completedAt", job.getCompletedAt().toString());
        }
        if (job.getResult() != null) {
            response.put("result", job.getResult());
        }
        if (job.getError() != null) {
            response.put("error", job.getError());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * SSE 订阅任务结果
     */
    @GetMapping(value = "/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamJobResult(@PathVariable String jobId) {
        Optional<AnalyzeJob> jobOpt = analyzeService.getJob(jobId);

        if (jobOpt.isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("Job not found: " + jobId)
                    .build());
        }

        AnalyzeJob job = jobOpt.get();

        // 如果任务已完成，直接返回结果
        if (job.isTerminal()) {
            String data = job.getStatus() == AnalyzeJob.JobStatus.DONE
                    ? job.getResult()
                    : "Error: " + job.getError();
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("result")
                            .data(data)
                            .build(),
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data(job.getStatus().name())
                            .build()
            );
        }

        // 等待任务完成
        return analyzeService.waitForResult(jobId)
                .map(result -> ServerSentEvent.<String>builder()
                        .event("result")
                        .data(result)
                        .build())
                .flux()
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("DONE")
                                .build()
                ))
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("error")
                                .data(e.getMessage())
                                .build()
                ))
                .timeout(Duration.ofMinutes(5));
    }

    /**
     * 取消任务
     */
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        boolean canceled = analyzeService.cancelJob(jobId);

        if (canceled) {
            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "status", "CANCELED"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "CANCEL_FAILED",
                    "message", "Job not found or already completed"
            ));
        }
    }

    /**
     * 获取最近的错误请求列表
     */
    @GetMapping("/recent-errors")
    public ResponseEntity<List<Map<String, Object>>> getRecentErrors(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "20") int limit) {

        List<RequestContext> errors = requestContextCache.getRecentErrors(serviceName, limit);

        List<Map<String, Object>> result = errors.stream().map(ctx -> {
            Map<String, Object> item = new HashMap<>();
            item.put("requestId", ctx.getRequestId());
            item.put("serviceName", ctx.getServiceName());
            item.put("path", ctx.getPath());
            item.put("method", ctx.getMethod());
            item.put("responseStatus", ctx.getResponseStatus());
            item.put("errorId", ctx.getErrorId());
            item.put("timestamp", ctx.getTimestamp() == null ? null : ctx.getTimestamp().toString());
            if (ctx.getErrorMeta() != null) {
                item.put("errorMeta", ctx.getErrorMeta());
            }
            return item;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 获取请求上下文缓存统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        RequestContextCacheService.CacheStats stats = requestContextCache.getStats();

        Map<String, Object> response = new HashMap<>();
        response.put("totalRequests", stats.totalCount());
        response.put("errorRequests", stats.errorCount());
        response.put("maxCapacity", stats.maxCapacity());

        return ResponseEntity.ok(response);
    }

    /**
     * 从微服务获取完整错误详情
     */
    @GetMapping("/error-detail/{serviceName}/{errorId}")
    public Mono<ResponseEntity<Object>> getErrorDetail(
            @PathVariable String serviceName,
            @PathVariable String errorId) {

        return errorCollectorProxy.fetchErrorDetail(serviceName, errorId)
                .map(detail -> ResponseEntity.ok((Object) detail))
                .onErrorResume(e -> {
                    log.error("Failed to fetch error detail", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "error", "NOT_FOUND",
                            "message", e.getMessage()
                    )));
                });
    }

}

