package com.example.message.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建消息请求 DTO
 */
@ApiModel(description = "创建消息请求")
public class MessageCreateRequest {

    @ApiModelProperty(value = "发送者ID", required = true, example = "1001")
    @NotNull(message = "发送者ID不能为空")
    private Long senderId;

    @ApiModelProperty(value = "接收者ID", required = true, example = "1002")
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    @ApiModelProperty(value = "消息标题", required = true, example = "系统通知")
    @NotBlank(message = "消息标题不能为空")
    private String title;

    @ApiModelProperty(value = "消息内容", required = true, example = "您有一条新的消息")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @ApiModelProperty(value = "消息类型（SYSTEM-系统消息，PRIVATE-私信，BROADCAST-广播）", required = false, example = "PRIVATE")
    private String messageType;

    @ApiModelProperty(value = "优先级（HIGH-高，NORMAL-普通，LOW-低）", required = false, example = "NORMAL")
    private String priority;

    // Getters and Setters
    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}

