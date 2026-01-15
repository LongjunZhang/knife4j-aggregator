package com.example.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户 JSON 请求
 */
@Schema(description = "用户 JSON 请求")
public class UserJsonRequest {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    private String username;

    @Schema(description = "年龄", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "25")
    private Integer age;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "13800138000")
    private String phone;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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
}

