package com.example.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户请求 DTO
 */
@Schema(description = "更新用户请求")
public class UserUpdateRequest {

    @Schema(description = "密码（留空不更新）", required = false, example = "newPassword123")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;

    @Schema(description = "邮箱地址", required = false, example = "newemail@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号码", required = false, example = "13900139000")
    private String phone;

    @Schema(description = "昵称", required = false, example = "新昵称")
    private String nickname;

    @Schema(description = "头像URL", required = false, example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "状态（0-禁用，1-启用）", required = false, example = "1")
    private Integer status;

    // Getters and Setters
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

