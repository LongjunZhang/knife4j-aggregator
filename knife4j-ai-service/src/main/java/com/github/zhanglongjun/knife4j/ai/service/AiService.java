package com.github.zhanglongjun.knife4j.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zhanglongjun.knife4j.ai.config.AiProperties;
import com.github.zhanglongjun.knife4j.ai.dto.ExplainErrorRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 服务实现
 * 
 * 提供流式错误解释功能 (SSE)
 */
@Slf4j
@Service
public class AiService {
    
    private final ChatClient chatClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    
    @Value("classpath:prompts/explain-error.system.txt")
    private Resource explainErrorPrompt;
    
    public AiService(ChatClient.Builder chatClientBuilder, AiProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 流式解释错误 - 逐字返回 (SSE)
     * 
     * 使用 ChatClient.stream() 实现真正的流式输出，
     * 像 ChatGPT 一样一个字一个字吐出来。
     * 
     * 自动过滤 AI 可能输出的 Markdown 代码块标记（```json 和 ```）
     */
    public Flux<String> explainErrorStream(ExplainErrorRequest request) {
        log.info("Starting SSE stream for explain-error: {} {} {}", 
            request.getServiceName(), request.getMethod(), request.getPath());
        
        try {
            // 打印 doc-aggregator 传递的原始错误对象
            logErrorMeta(request);
            
            String systemPrompt = loadPromptTemplate(explainErrorPrompt);
            String userPrompt = buildExplainErrorUserPrompt(request);
            
            // 使用 stream() 获取流式响应，并过滤 Markdown 标记
            return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .transform(this::filterMarkdownCodeBlock)
                .doOnNext(chunk -> log.trace("SSE chunk (filtered): {}", chunk))
                .doOnComplete(() -> log.info("SSE stream completed"))
                .doOnError(e -> log.error("SSE stream error", e));
                
        } catch (Exception e) {
            log.error("Failed to start SSE stream", e);
            return Flux.error(e);
        }
    }
    
    /**
     * 过滤流式输出中的 Markdown 代码块标记
     * 
     * AI 模型经常会在 JSON 输出前后添加 ```json 和 ``` 标记，
     * 此方法在流式传输过程中实时过滤这些标记。
     */
    private Flux<String> filterMarkdownCodeBlock(Flux<String> source) {
        // 使用状态机来处理流式过滤
        return Flux.create(sink -> {
            final StringBuilder buffer = new StringBuilder();
            final AtomicBoolean headerProcessed = new AtomicBoolean(false);
            
            source.subscribe(
                chunk -> {
                    if (!headerProcessed.get()) {
                        // 累积内容直到可以判断是否有 Markdown 标记
                        buffer.append(chunk);
                        String content = buffer.toString();
                        
                        // 检查是否以 ```json 或 ``` 开头
                        if (content.startsWith("```json")) {
                            // 找到开头标记，跳过它
                            int newlineIdx = content.indexOf('\n', 7);
                            if (newlineIdx != -1) {
                                headerProcessed.set(true);
                                String remaining = content.substring(newlineIdx + 1);
                                if (!remaining.isEmpty()) {
                                    sink.next(remaining);
                                }
                                buffer.setLength(0);
                            }
                            // 否则继续累积，等待换行符
                        } else if (content.startsWith("```\n") || content.startsWith("```\r\n")) {
                            // 简单的 ``` 开头
                            headerProcessed.set(true);
                            int newlineIdx = content.indexOf('\n');
                            String remaining = content.substring(newlineIdx + 1);
                            if (!remaining.isEmpty()) {
                                sink.next(remaining);
                            }
                            buffer.setLength(0);
                        } else if (content.startsWith("{")) {
                            // 直接以 { 开头，无 Markdown 标记
                            headerProcessed.set(true);
                            sink.next(content);
                            buffer.setLength(0);
                        } else if (content.length() > 10 && !content.startsWith("`")) {
                            // 内容足够长且不以 ` 开头，认为无 Markdown 标记
                            headerProcessed.set(true);
                            sink.next(content);
                            buffer.setLength(0);
                        }
                        // 否则继续累积
                    } else {
                        // 头部已处理，直接输出
                        sink.next(chunk);
                    }
                },
                sink::error,
                () -> {
                    // 流结束时，处理缓冲区中剩余的内容
                    if (buffer.length() > 0) {
                        String content = buffer.toString();
                        // 移除可能存在的开头标记
                        if (content.startsWith("```json\n")) {
                            content = content.substring(8);
                        } else if (content.startsWith("```json\r\n")) {
                            content = content.substring(9);
                        } else if (content.startsWith("```\n")) {
                            content = content.substring(4);
                        } else if (content.startsWith("```\r\n")) {
                            content = content.substring(5);
                        }
                        if (!content.isEmpty()) {
                            sink.next(content);
                        }
                    }
                    sink.complete();
                }
            );
        }).map(obj -> {
            // 移除结尾的 ``` 标记
            String s = (String) obj;
            // 检查是否以 ``` 结尾（可能带换行）
            String trimmed = s.stripTrailing();
            if (trimmed.endsWith("```")) {
                return trimmed.substring(0, trimmed.length() - 3);
            }
            return s;
        }).filter(s -> !s.isEmpty());
    }
    
    private String loadPromptTemplate(Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 构建发送给 AI 的用户提示词
     * 
     * 如果有 _errorMeta（来自 error-collector），则构建符合 explain-error.system.txt 提示词期望的 JSON 格式。
     * 否则使用文本格式作为 fallback。
     */
    private String buildExplainErrorUserPrompt(ExplainErrorRequest request) throws JsonProcessingException {
        // 优先使用 _errorMeta 构建符合提示词期望的 JSON 格式
        if (request.get_errorMeta() != null && !request.get_errorMeta().isEmpty()) {
            Map<String, Object> errorJson = new LinkedHashMap<>();
            errorJson.put("_errorMeta", request.get_errorMeta());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorJson);
        }
        
        // Fallback: 如果没有 _errorMeta，使用文本格式
        log.warn("No _errorMeta found in request, using fallback text format");
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下 API 请求错误：\n\n");
        sb.append("## 接口信息\n");
        sb.append("- 服务名: ").append(request.getServiceName()).append("\n");
        sb.append("- 方法: ").append(request.getMethod()).append("\n");
        sb.append("- 路径: ").append(request.getPath()).append("\n");
        
        if (request.getSummary() != null) {
            sb.append("- 摘要: ").append(request.getSummary()).append("\n");
        }
        
        if (request.getResponse() != null) {
            sb.append("\n## 响应信息\n");
            sb.append("- Status: ").append(request.getResponse().getStatus()).append("\n");
            
            if (request.getResponse().getBody() != null) {
                sb.append("- Body: ").append(objectMapper.writeValueAsString(request.getResponse().getBody())).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 打印 doc-aggregator 传递的原始错误对象 (_errorMeta)
     */
    private void logErrorMeta(ExplainErrorRequest request) {
        try {
            if (request.get_errorMeta() != null && !request.get_errorMeta().isEmpty()) {
                String errorMetaJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request.get_errorMeta());
                log.info("====== [AI-REQUEST-SSE] doc-aggregator 传递的 _errorMeta ======");
                log.info("{}", errorMetaJson);
                log.info("====== [AI-REQUEST-SSE] END ======");
            } else {
                log.warn("[AI-REQUEST-SSE] 请求中没有 _errorMeta，无法获取 error-collector 原始错误信息");
            }
        } catch (Exception e) {
            log.warn("打印 _errorMeta 失败", e);
        }
    }
}
