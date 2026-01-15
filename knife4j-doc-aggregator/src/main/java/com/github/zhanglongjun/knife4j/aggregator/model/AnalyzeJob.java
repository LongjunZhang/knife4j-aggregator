package com.github.zhanglongjun.knife4j.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * AI 分析任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeJob {

    /**
     * 任务 ID
     */
    private String jobId;

    /**
     * 关联的请求 ID
     */
    private String requestId;

    /**
     * 关联的错误 ID
     */
    private String errorId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 任务状态
     */
    private JobStatus status;

    /**
     * 分析结果
     */
    private String result;

    /**
     * 错误信息（如果分析失败）
     */
    private String error;

    /**
     * 请求上下文摘要（发给 AI 的内容）
     */
    private Map<String, Object> contextSummary;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 开始时间
     */
    private Instant startedAt;

    /**
     * 完成时间
     */
    private Instant completedAt;

    /**
     * 任务状态枚举
     */
    public enum JobStatus {
        /**
         * 等待中
         */
        PENDING,
        /**
         * 运行中
         */
        RUNNING,
        /**
         * 已完成
         */
        DONE,
        /**
         * 已取消
         */
        CANCELED,
        /**
         * 失败
         */
        FAILED
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return status == JobStatus.DONE
                || status == JobStatus.CANCELED
                || status == JobStatus.FAILED;
    }

}

