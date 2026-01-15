package com.github.zhanglongjun.knife4j.aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.aggregator.config.AiProxyProperties;
import com.github.zhanglongjun.knife4j.aggregator.model.AnalyzeJob;
import com.github.zhanglongjun.knife4j.aggregator.model.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误分析服务
 * 负责管理 AI 分析任务、调用 AI 服务、支持取消
 */
@Slf4j
@Service
public class ErrorAnalyzeService {

    private final AiProxyProperties aiProperties;
    private final RequestContextCacheService requestContextCache;
    private final ErrorCollectorProxyService errorCollectorProxy;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * 任务存储
     */
    private final Map<String, AnalyzeJob> jobs = new ConcurrentHashMap<>();

    /**
     * 任务结果 Sink（用于 SSE 推送）
     */
    private final Map<String, Sinks.One<String>> jobSinks = new ConcurrentHashMap<>();

    /**
     * errorId -> jobId 映射（用于幂等）
     */
    private final Map<String, String> errorIdToJobId = new ConcurrentHashMap<>();

    public ErrorAnalyzeService(
            AiProxyProperties aiProperties,
            RequestContextCacheService requestContextCache,
            ErrorCollectorProxyService errorCollectorProxy) {
        this.aiProperties = aiProperties;
        this.requestContextCache = requestContextCache;
        this.errorCollectorProxy = errorCollectorProxy;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(aiProperties.getServiceUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 提交分析任务
     * 幂等：同一个 errorId 返回已有任务
     *
     * @param requestId 请求 ID
     * @param errorId   错误 ID
     * @return 任务
     */
    public Mono<AnalyzeJob> submitAnalyzeJob(String requestId, String errorId) {
        // 幂等检查
        String existingJobId = errorIdToJobId.get(errorId);
        if (existingJobId != null) {
            AnalyzeJob existingJob = jobs.get(existingJobId);
            if (existingJob != null && !existingJob.isTerminal()) {
                log.info("Returning existing job for errorId={}: jobId={}", errorId, existingJobId);
                return Mono.just(existingJob);
            }
        }

        // 获取请求上下文
        Optional<RequestContext> contextOpt = requestContextCache.get(requestId);
        if (contextOpt.isEmpty()) {
            contextOpt = requestContextCache.getByErrorId(errorId);
        }

        if (contextOpt.isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "Request context not found: requestId=" + requestId + ", errorId=" + errorId));
        }

        RequestContext context = contextOpt.get();

        // 创建任务
        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AnalyzeJob job = AnalyzeJob.builder()
                .jobId(jobId)
                .requestId(requestId)
                .errorId(errorId)
                .serviceName(context.getServiceName())
                .status(AnalyzeJob.JobStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        jobs.put(jobId, job);
        errorIdToJobId.put(errorId, jobId);

        // 创建结果 Sink
        Sinks.One<String> sink = Sinks.one();
        jobSinks.put(jobId, sink);

        // 异步执行分析
        executeAnalysis(job, context)
                .subscribe(
                        result -> {
                            job.setResult(result);
                            job.setStatus(AnalyzeJob.JobStatus.DONE);
                            job.setCompletedAt(Instant.now());
                            sink.tryEmitValue(result);
                        },
                        error -> {
                            job.setError(error.getMessage());
                            job.setStatus(AnalyzeJob.JobStatus.FAILED);
                            job.setCompletedAt(Instant.now());
                            sink.tryEmitError(error);
                        }
                );

        log.info("Created analyze job: jobId={}, errorId={}", jobId, errorId);
        return Mono.just(job);
    }

    /**
     * 获取任务状态
     */
    public Optional<AnalyzeJob> getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    /**
     * 获取任务结果（阻塞等待）
     */
    public Mono<String> waitForResult(String jobId) {
        Sinks.One<String> sink = jobSinks.get(jobId);
        if (sink == null) {
            AnalyzeJob job = jobs.get(jobId);
            if (job != null && job.getResult() != null) {
                return Mono.just(job.getResult());
            }
            return Mono.error(new IllegalArgumentException("Job not found: " + jobId));
        }

        return sink.asMono().timeout(aiProperties.getTimeout());
    }

    /**
     * 取消任务
     */
    public boolean cancelJob(String jobId) {
        AnalyzeJob job = jobs.get(jobId);
        if (job == null) {
            return false;
        }

        if (job.isTerminal()) {
            return false;
        }

        job.setStatus(AnalyzeJob.JobStatus.CANCELED);
        job.setCompletedAt(Instant.now());

        Sinks.One<String> sink = jobSinks.get(jobId);
        if (sink != null) {
            sink.tryEmitError(new RuntimeException("Job canceled"));
        }

        log.info("Canceled job: jobId={}", jobId);
        return true;
    }

    /**
     * 执行分析
     */
    private Mono<String> executeAnalysis(AnalyzeJob job, RequestContext context) {
        job.setStatus(AnalyzeJob.JobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        // 获取完整错误详情
        return errorCollectorProxy.fetchErrorDetail(context.getServiceName(), context.getErrorId())
                .flatMap(errorDetail -> {
                    // 构建分析请求
                    Map<String, Object> request = buildAnalyzeRequest(context, errorDetail);
                    job.setContextSummary(request);

                    // 调用 AI 服务
                    return callAiService(request);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch error detail, analyzing with basic info", e);
                    // 获取失败时使用基本信息
                    Map<String, Object> request = buildAnalyzeRequest(context, null);
                    job.setContextSummary(request);
                    return callAiService(request);
                });
    }

    /**
     * 构建分析请求
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildAnalyzeRequest(RequestContext context, Map<String, Object> errorDetail) {
        Map<String, Object> request = new HashMap<>();

        request.put("serviceName", context.getServiceName());
        request.put("path", context.getPath());
        request.put("method", context.getMethod());
        request.put("requestParams", context.getRequestParams());
        request.put("responseStatus", context.getResponseStatus());

        // 错误元数据
        if (context.getErrorMeta() != null) {
            request.put("errorMeta", context.getErrorMeta());
        }

        // 完整错误详情
        if (errorDetail != null) {
            request.put("errorDetail", errorDetail);

            // 提取关键信息
            Object stackTrace = errorDetail.get("filteredStackTrace");
            if (stackTrace != null) {
                request.put("stackTrace", stackTrace);
            }

            Object causeChain = errorDetail.get("causeChain");
            if (causeChain != null) {
                request.put("causeChain", causeChain);
            }
        }

        return request;
    }

    /**
     * 调用 AI 服务
     */
    @SuppressWarnings("unchecked")
    private Mono<String> callAiService(Map<String, Object> request) {
        return webClient.post()
                .uri("/api/ai/explain-error")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(aiProperties.getTimeout())
                .map(response -> {
                    Object result = response.get("result");
                    if (result != null) {
                        return result.toString();
                    }
                    Object explanation = response.get("explanation");
                    if (explanation != null) {
                        return explanation.toString();
                    }
                    try {
                        return objectMapper.writeValueAsString(response);
                    } catch (Exception e) {
                        return response.toString();
                    }
                })
                .doOnError(e -> log.error("AI service call failed", e));
    }

    /**
     * 清理过期任务
     */
    public void cleanupExpiredJobs() {
        Instant threshold = Instant.now().minusSeconds(3600); // 1 小时前
        jobs.entrySet().removeIf(entry -> {
            AnalyzeJob job = entry.getValue();
            return job.isTerminal() && job.getCompletedAt() != null
                    && job.getCompletedAt().isBefore(threshold);
        });
    }

}

