package com.github.zhanglongjun.knife4j.error.javax.controller;

import com.github.zhanglongjun.knife4j.error.config.ErrorCollectorProperties;
import com.github.zhanglongjun.knife4j.error.model.ErrorDetail;
import com.github.zhanglongjun.knife4j.error.store.ErrorDetailStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 错误详情查询接口 (Javax 版本)
 * 内部 API，用于查询错误详情
 */
@Slf4j
@RestController
@RequestMapping("${knife4j.error-collector.internal-api-prefix:/internal/error-collector}")
public class ErrorDetailController {

    private final ErrorCollectorProperties properties;
    private final ErrorDetailStore errorStore;

    public ErrorDetailController(
            ErrorCollectorProperties properties,
            ErrorDetailStore errorStore) {
        this.properties = properties;
        this.errorStore = errorStore;
    }

    /**
     * 根据 errorId 获取完整错误详情
     */
    @GetMapping("/errors/{errorId}")
    public ResponseEntity<?> getErrorDetail(@PathVariable String errorId) {
        Optional<ErrorDetail> detail = errorStore.get(errorId);
        if (!detail.isPresent()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Error detail not found or expired");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(detail.get());
    }

    /**
     * 获取存储状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.isEnabled());
        status.put("store", errorStore.getStatus());

        return ResponseEntity.ok(status);
    }

}
