package com.example.message.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 更新消息请求 DTO
 */
@ApiModel(description = "更新消息请求")
public class MessageUpdateRequest {

    @ApiModelProperty(value = "消息标题", required = false, example = "更新后的标题")
    private String title;

    @ApiModelProperty(value = "消息内容", required = false, example = "更新后的内容")
    private String content;

    @ApiModelProperty(value = "优先级（HIGH-高，NORMAL-普通，LOW-低）", required = false, example = "HIGH")
    private String priority;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}

