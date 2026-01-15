package com.example.message.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 消息视图对象
 */
@ApiModel(description = "消息信息")
public class MessageVO {

    @ApiModelProperty(value = "消息ID", example = "MSG001")
    private String messageId;

    @ApiModelProperty(value = "发送者ID", example = "1001")
    private Long senderId;

    @ApiModelProperty(value = "发送者名称", example = "系统管理员")
    private String senderName;

    @ApiModelProperty(value = "接收者ID", example = "1002")
    private Long receiverId;

    @ApiModelProperty(value = "接收者名称", example = "张三")
    private String receiverName;

    @ApiModelProperty(value = "消息标题", example = "系统通知")
    private String title;

    @ApiModelProperty(value = "消息内容", example = "您有一条新的消息")
    private String content;

    @ApiModelProperty(value = "消息类型（SYSTEM-系统消息，PRIVATE-私信，BROADCAST-广播）", example = "PRIVATE")
    private String messageType;

    @ApiModelProperty(value = "优先级（HIGH-高，NORMAL-普通，LOW-低）", example = "NORMAL")
    private String priority;

    @ApiModelProperty(value = "阅读状态（true-已读，false-未读）", example = "false")
    private Boolean read;

    @ApiModelProperty(value = "创建时间", example = "2024-01-15T10:30:00")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "阅读时间", example = "2024-01-15T11:00:00")
    private LocalDateTime readTime;

    // 静态工厂方法
    public static MessageVO mock(String messageId) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(messageId);
        vo.setSenderId(1001L);
        vo.setSenderName("系统管理员");
        vo.setReceiverId(1002L);
        vo.setReceiverName("张三");
        vo.setTitle("系统通知");
        vo.setContent("您有一条新的消息，请及时查看。");
        vo.setMessageType("SYSTEM");
        vo.setPriority("NORMAL");
        vo.setRead(false);
        vo.setCreateTime(LocalDateTime.now().minusMinutes(30));
        return vo;
    }

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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
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

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }
}

