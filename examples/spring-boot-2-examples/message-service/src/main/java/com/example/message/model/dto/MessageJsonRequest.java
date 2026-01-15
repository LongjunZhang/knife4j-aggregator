package com.example.message.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 消息 JSON 请求
 */
@ApiModel(description = "消息 JSON 请求")
public class MessageJsonRequest {

    @ApiModelProperty(value = "消息标题", required = true, example = "系统通知")
    private String title;

    @ApiModelProperty(value = "消息内容", required = true, example = "您有一条新的系统消息")
    private String content;

    @ApiModelProperty(value = "接收者ID", required = true, example = "10001")
    private Long receiverId;

    @ApiModelProperty(value = "消息类型", required = false, example = "NOTICE")
    private String messageType;

    @ApiModelProperty(value = "是否紧急", required = false, example = "false")
    private Boolean urgent;

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

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }
}

