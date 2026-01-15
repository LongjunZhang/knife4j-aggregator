package com.example.user.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户 XML 请求
 */
@Schema(description = "用户 XML 请求")
@JacksonXmlRootElement(localName = "userRequest")
public class UserXmlRequest {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @JacksonXmlProperty(localName = "userId")
    private Long userId;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "小明")
    @JacksonXmlProperty(localName = "nickname")
    private String nickname;

    @Schema(description = "用户等级", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "VIP")
    @JacksonXmlProperty(localName = "level")
    private String level;

    @Schema(description = "积分", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1000")
    @JacksonXmlProperty(localName = "points")
    private Integer points;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}

