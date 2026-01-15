package com.example.user.controller;

import com.example.user.model.dto.UserCreateRequest;
import com.example.user.model.dto.UserUpdateRequest;
import com.example.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户管理接口
 * 提供用户的增删改查操作（GET/POST/PUT/PATCH/DELETE）
 */
@Tag(name = "用户管理", description = "用户 CRUD 操作接口")
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "分页查询用户列表")
    @GetMapping("/list")
    public List<UserVO> list(
            @Parameter(description = "页码（必须大于0）", example = "1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,

            @Parameter(description = "每页大小（必须大于0）", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer pageSize,

            @Parameter(description = "用户名（模糊查询）", example = "zhang")
            @RequestParam(required = false) String username,

            @Parameter(description = "状态筛选", example = "1")
            @RequestParam(required = false) Integer status
    ) {
        // 模拟返回数据
        List<UserVO> users = new ArrayList<>();
        for (long i = 1; i <= pageSize; i++) {
            users.add(UserVO.mock((pageNum - 1L) * pageSize + i));
        }
        return users;
    }

    /**
     * 根据 ID 获取用户
     */
    @Operation(summary = "获取用户详情", description = "根据用户 ID 获取用户详细信息")
    @GetMapping("/{id}")
    public UserVO getById(
            @Parameter(description = "用户ID（必须大于0）", required = true, example = "1001")
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id
    ) {
        return UserVO.mock(id);
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserVO create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户创建请求体",
                    required = true
            )
            @Valid @RequestBody UserCreateRequest request
    ) {
        // 模拟创建用户
        UserVO vo = new UserVO();
        vo.setId(System.currentTimeMillis());
        vo.setUsername(request.getUsername());
        vo.setEmail(request.getEmail());
        vo.setPhone(request.getPhone());
        vo.setNickname(request.getNickname());
        vo.setStatus(1);
        return vo;
    }

    /**
     * 更新用户（全量）
     */
    @Operation(summary = "更新用户（全量）", description = "根据 ID 更新用户所有可更新字段")
    @PutMapping("/{id}")
    public UserVO update(
            @Parameter(description = "用户ID", required = true, example = "1001")
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户更新请求体",
                    required = true
            )
            @Valid @RequestBody UserUpdateRequest request
    ) {
        // 模拟更新用户
        UserVO vo = UserVO.mock(id);
        if (request.getEmail() != null) vo.setEmail(request.getEmail());
        if (request.getPhone() != null) vo.setPhone(request.getPhone());
        if (request.getNickname() != null) vo.setNickname(request.getNickname());
        if (request.getAvatar() != null) vo.setAvatar(request.getAvatar());
        if (request.getStatus() != null) vo.setStatus(request.getStatus());
        return vo;
    }

    /**
     * 更新用户（部分字段）
     */
    @Operation(summary = "更新用户（部分）", description = "根据 ID 更新用户部分字段，仅更新传入的非空字段")
    @PatchMapping("/{id}")
    public UserVO partialUpdate(
            @Parameter(description = "用户ID", required = true, example = "1001")
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "部分更新字段",
                    required = true
            )
            @RequestBody Map<String, Object> updates
    ) {
        // 模拟部分更新
        UserVO vo = UserVO.mock(id);
        if (updates.containsKey("nickname")) {
            vo.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("status")) {
            vo.setStatus((Integer) updates.get("status"));
        }
        return vo;
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "根据 ID 删除用户")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "用户ID", required = true, example = "1001")
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id
    ) {
        // 模拟删除用户
        // 实际业务中执行删除逻辑
    }

    // ==================== 异常测试接口 ====================

    /**
     * 测试空指针异常
     */
    @Operation(summary = "测试空指针异常", description = "触发 NullPointerException")
    @GetMapping("/exception/npe")
    public String testNullPointerException() {
        String s = null;
        return s.length() + ""; // NullPointerException
    }

    /**
     * 测试数字格式异常
     */
    @Operation(summary = "测试数字格式异常", description = "传入非数字字符串触发 NumberFormatException")
    @GetMapping("/exception/number-format")
    public Integer testNumberFormatException(
            @Parameter(description = "数字字符串（传入非数字触发异常）", example = "abc")
            @RequestParam @NotBlank(message = "数字不能为空") String number
    ) {
        return Integer.parseInt(number); // NumberFormatException
    }

    /**
     * 测试非法参数异常
     */
    @Operation(summary = "测试非法参数异常", description = "传入负数触发 IllegalArgumentException")
    @GetMapping("/exception/illegal-argument")
    public String testIllegalArgumentException(
            @Parameter(description = "数值（传入负数触发异常）", example = "-1")
            @RequestParam Integer value
    ) {
        if (value < 0) {
            throw new IllegalArgumentException("值不能为负数: " + value);
        }
        return "值为: " + value;
    }

    /**
     * 测试索引越界异常
     */
    @Operation(summary = "测试索引越界异常", description = "触发 IndexOutOfBoundsException")
    @GetMapping("/exception/index-out-of-bounds")
    public String testIndexOutOfBoundsException() {
        List<String> list = new ArrayList<>();
        return list.get(10); // IndexOutOfBoundsException
    }

    /**
     * 测试算术异常
     */
    @Operation(summary = "测试算术异常", description = "传入0作为除数触发 ArithmeticException")
    @GetMapping("/exception/arithmetic")
    public Integer testArithmeticException(
            @Parameter(description = "除数（传入0触发异常）", example = "0")
            @RequestParam Integer divisor
    ) {
        return 100 / divisor; // ArithmeticException when divisor=0
    }

    /**
     * 测试类型转换异常
     */
    @Operation(summary = "测试类型转换异常", description = "触发 ClassCastException")
    @GetMapping("/exception/class-cast")
    public Integer testClassCastException() {
        Object obj = "string";
        return (Integer) obj; // ClassCastException
    }
}
