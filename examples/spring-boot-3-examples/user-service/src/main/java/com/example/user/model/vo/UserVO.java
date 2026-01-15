package com.example.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID", example = "1001")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "状态（0-禁用，1-启用）", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2024-01-15T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-15T15:45:00")
    private LocalDateTime updateTime;

    // 静态工厂方法
    public static UserVO mock(Long id) {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUsername("user_" + id);
        vo.setEmail("user" + id + "@example.com");
        vo.setPhone("138" + String.format("%08d", id));
        vo.setNickname("用户" + id);
        vo.setAvatar("https://example.com/avatars/" + id + ".png");
        vo.setStatus(1);
        vo.setCreateTime(LocalDateTime.now().minusDays(id % 30));
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

