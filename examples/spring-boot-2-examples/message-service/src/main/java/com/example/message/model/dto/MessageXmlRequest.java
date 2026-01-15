package com.example.message.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 消息 XML 请求
 */
@ApiModel(description = "消息 XML 请求")
@JacksonXmlRootElement(localName = "messageRequest")
public class MessageXmlRequest {

    @ApiModelProperty(value = "消息ID", required = true, example = "MSG001")
    @JacksonXmlProperty(localName = "messageId")
    private String messageId;

    @ApiModelProperty(value = "发送者ID", required = true, example = "1001")
    @JacksonXmlProperty(localName = "senderId")
    private Long senderId;

    @ApiModelProperty(value = "主题", required = false, example = "重要通知")
    @JacksonXmlProperty(localName = "subject")
    private String subject;

    @ApiModelProperty(value = "优先级", required = false, example = "HIGH")
    @JacksonXmlProperty(localName = "priority")
    private String priority;

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}

