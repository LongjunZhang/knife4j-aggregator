package com.example.user.config;

import com.example.user.model.vo.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.stream.Collectors;

/**
 * 全局响应包装器 + 异常处理器
 * 1. 自动将 Controller 返回值包装为 Result 对象
 * 2. 统一处理各类异常并返回 Result 对象
 */
@RestControllerAdvice(basePackages = "com.example.user.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(GlobalResponseAdvice.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== ResponseBodyAdvice 实现 ====================

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }

        if (isSwaggerRequest(request)) {
            return body;
        }

        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                return Result.error("响应序列化失败");
            }
        }

        return Result.success(body);
    }

    private boolean isSwaggerRequest(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.contains("/v3/api-docs") ||
               path.contains("/v2/api-docs") ||
               path.contains("/swagger") ||
               path.contains("/webjars");
    }

    // ==================== 参数校验异常处理 ====================

    /**
     * 处理 @RequestBody 参数校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 (RequestBody): {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理 @ModelAttribute 参数绑定校验失败异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理 @RequestParam/@PathVariable 参数校验失败异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return Result.badRequest(message);
    }

    // ==================== 常见运行时异常处理 ====================

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleNullPointerException(NullPointerException ex) {
        log.error("空指针异常", ex);
        return Result.error(500, "系统错误：空指针异常 - " + getExceptionMessage(ex));
    }

    /**
     * 处理数字格式异常
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNumberFormatException(NumberFormatException ex) {
        log.warn("数字格式异常: {}", ex.getMessage());
        return Result.badRequest("数字格式错误：" + ex.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数异常: {}", ex.getMessage());
        return Result.badRequest("非法参数：" + ex.getMessage());
    }

    /**
     * 处理数组越界异常
     */
    @ExceptionHandler(IndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleIndexOutOfBoundsException(IndexOutOfBoundsException ex) {
        log.error("索引越界异常", ex);
        return Result.error(500, "系统错误：索引越界 - " + ex.getMessage());
    }

    /**
     * 处理算术异常（如除零）
     */
    @ExceptionHandler(ArithmeticException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleArithmeticException(ArithmeticException ex) {
        log.error("算术异常", ex);
        return Result.error(500, "计算错误：" + ex.getMessage());
    }

    /**
     * 处理类型转换异常
     */
    @ExceptionHandler(ClassCastException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleClassCastException(ClassCastException ex) {
        log.error("类型转换异常", ex);
        return Result.error(500, "类型转换错误：" + ex.getMessage());
    }

    // ==================== 兜底异常处理 ====================

    /**
     * 处理所有未被捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception ex) {
        log.error("未知异常", ex);
        return Result.error(500, "系统异常：" + getExceptionMessage(ex));
    }

    /**
     * 获取异常消息，处理消息为空的情况
     */
    private String getExceptionMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}
