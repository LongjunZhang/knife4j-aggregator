package com.example.message.controller;

import com.example.message.model.dto.MessageCreateRequest;
import com.example.message.model.dto.MessageUpdateRequest;
import com.example.message.model.vo.MessageVO;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息管理接口
 * 提供消息的增删改查操作（GET/POST/PUT/PATCH/DELETE）
 * 使用 Swagger 2 注解风格
 */
@Api(tags = "消息管理", value = "消息 CRUD 操作接口")
@Validated
@RestController
@RequestMapping("/message")
public class MessageController {

    /**
     * 获取消息列表
     */
    @ApiOperation(value = "获取消息列表", notes = "分页查询消息列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码（必须大于0）", dataType = "Integer", paramType = "query", defaultValue = "1", example = "1"),
            @ApiImplicitParam(name = "pageSize", value = "每页大小（必须大于0）", dataType = "Integer", paramType = "query", defaultValue = "10", example = "10"),
            @ApiImplicitParam(name = "receiverId", value = "接收者ID", dataType = "Long", paramType = "query", required = false, example = "1002"),
            @ApiImplicitParam(name = "read", value = "是否已读", dataType = "Boolean", paramType = "query", required = false, example = "false")
    })
    @GetMapping("/list")
    public List<MessageVO> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer pageSize,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) Boolean read
    ) {
        // 模拟返回数据
        List<MessageVO> messages = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            String messageId = String.format("MSG%03d", (pageNum - 1) * pageSize + i);
            messages.add(MessageVO.mock(messageId));
        }
        return messages;
    }

    /**
     * 根据消息ID获取消息
     */
    @ApiOperation(value = "获取消息详情", notes = "根据消息ID获取消息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "messageId", value = "消息ID（不能为空）", dataType = "String", paramType = "path", required = true, example = "MSG001")
    })
    @GetMapping("/{messageId}")
    public MessageVO getById(
            @PathVariable @NotBlank(message = "消息ID不能为空") String messageId
    ) {
        return MessageVO.mock(messageId);
    }

    /**
     * 发送消息
     */
    @ApiOperation(value = "发送消息", notes = "创建并发送新消息")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageVO create(
            @ApiParam(value = "消息创建请求体", required = true)
            @Valid @RequestBody MessageCreateRequest request
    ) {
        // 模拟创建消息
        String messageId = String.format("MSG%d", System.currentTimeMillis() % 1000000);
        MessageVO vo = new MessageVO();
        vo.setMessageId(messageId);
        vo.setSenderId(request.getSenderId());
        vo.setSenderName("用户" + request.getSenderId());
        vo.setReceiverId(request.getReceiverId());
        vo.setReceiverName("用户" + request.getReceiverId());
        vo.setTitle(request.getTitle());
        vo.setContent(request.getContent());
        vo.setMessageType(request.getMessageType() != null ? request.getMessageType() : "PRIVATE");
        vo.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        vo.setRead(false);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 更新消息内容
     */
    @ApiOperation(value = "更新消息", notes = "根据消息ID更新消息内容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "messageId", value = "消息ID", dataType = "String", paramType = "path", required = true, example = "MSG001")
    })
    @PutMapping("/{messageId}")
    public MessageVO update(
            @PathVariable @NotBlank(message = "消息ID不能为空") String messageId,
            @ApiParam(value = "消息更新请求体", required = true)
            @Valid @RequestBody MessageUpdateRequest request
    ) {
        // 模拟更新消息
        MessageVO vo = MessageVO.mock(messageId);
        if (request.getTitle() != null) vo.setTitle(request.getTitle());
        if (request.getContent() != null) vo.setContent(request.getContent());
        if (request.getPriority() != null) vo.setPriority(request.getPriority());
        return vo;
    }

    /**
     * 标记消息已读
     */
    @ApiOperation(value = "标记消息已读", notes = "根据消息ID标记消息为已读状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "messageId", value = "消息ID", dataType = "String", paramType = "path", required = true, example = "MSG001")
    })
    @PatchMapping("/{messageId}/read")
    public MessageVO markAsRead(
            @PathVariable @NotBlank(message = "消息ID不能为空") String messageId,
            @ApiParam(value = "已读状态", required = false)
            @RequestBody(required = false) Map<String, Boolean> readStatus
    ) {
        // 模拟标记已读
        MessageVO vo = MessageVO.mock(messageId);
        vo.setRead(true);
        vo.setReadTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 删除消息
     */
    @ApiOperation(value = "删除消息", notes = "根据消息ID删除消息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "messageId", value = "消息ID", dataType = "String", paramType = "path", required = true, example = "MSG001")
    })
    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @NotBlank(message = "消息ID不能为空") String messageId
    ) {
        // 模拟删除消息
        // 实际业务中执行删除逻辑
    }

    // ==================== 异常测试接口 ====================

    /**
     * 测试空指针异常
     */
    @ApiOperation(value = "测试空指针异常", notes = "触发 NullPointerException")
    @GetMapping("/exception/npe")
    public String testNullPointerException() {
        String s = null;
        return s.length() + ""; // NullPointerException
    }

    /**
     * 测试数字格式异常
     */
    @ApiOperation(value = "测试数字格式异常", notes = "传入非数字字符串触发 NumberFormatException")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "number", value = "数字字符串（传入非数字触发异常）", dataType = "String", paramType = "query", required = true, example = "abc")
    })
    @GetMapping("/exception/number-format")
    public Integer testNumberFormatException(
            @RequestParam @NotBlank(message = "数字不能为空") String number
    ) {
        return Integer.parseInt(number); // NumberFormatException
    }

    /**
     * 测试非法参数异常
     */
    @ApiOperation(value = "测试非法参数异常", notes = "传入负数触发 IllegalArgumentException")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "数值（传入负数触发异常）", dataType = "Integer", paramType = "query", required = true, example = "-1")
    })
    @GetMapping("/exception/illegal-argument")
    public String testIllegalArgumentException(
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
    @ApiOperation(value = "测试索引越界异常", notes = "触发 IndexOutOfBoundsException")
    @GetMapping("/exception/index-out-of-bounds")
    public String testIndexOutOfBoundsException() {
        List<String> list = new ArrayList<>();
        return list.get(10); // IndexOutOfBoundsException
    }

    /**
     * 测试算术异常
     */
    @ApiOperation(value = "测试算术异常", notes = "传入0作为除数触发 ArithmeticException")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "divisor", value = "除数（传入0触发异常）", dataType = "Integer", paramType = "query", required = true, example = "0")
    })
    @GetMapping("/exception/arithmetic")
    public Integer testArithmeticException(
            @RequestParam Integer divisor
    ) {
        return 100 / divisor; // ArithmeticException when divisor=0
    }

    /**
     * 测试类型转换异常
     */
    @ApiOperation(value = "测试类型转换异常", notes = "触发 ClassCastException")
    @GetMapping("/exception/class-cast")
    public Integer testClassCastException() {
        Object obj = "string";
        return (Integer) obj; // ClassCastException
    }
}
