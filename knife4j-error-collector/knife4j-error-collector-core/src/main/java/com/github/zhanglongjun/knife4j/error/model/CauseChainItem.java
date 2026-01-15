package com.github.zhanglongjun.knife4j.error.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异常链中的单个异常项
 * 用于表示异常链中每一层的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CauseChainItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 异常类的完整类名
     */
    private String exceptionClass;

    /**
     * 异常消息（已脱敏）
     */
    private String message;

    /**
     * 在异常链中的层级，0 表示顶层异常
     */
    private int level;

    /**
     * 发生异常的代码位置（类名:方法名:行号）
     */
    private String location;

}

