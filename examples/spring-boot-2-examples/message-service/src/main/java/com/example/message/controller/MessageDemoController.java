package com.example.message.controller;

import com.example.message.model.dto.MessageJsonRequest;
import com.example.message.model.dto.MessageXmlRequest;
import com.example.message.model.vo.Result;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息演示接口
 * 演示不同请求参数类型：form-data, x-www-form-urlencoded, JSON, XML, Text, Binary
 * 使用 Swagger 2 注解风格
 */
// @Api(tags = "消息演示接口", value = "演示不同请求参数类型（Swagger 2 风格）")
// @RestController
// @RequestMapping("/message/demo")
public class MessageDemoController {

    /**
     * 1. form-data 请求
     */
    @ApiOperation(value = "Form-Data 发送消息", notes = "演示 multipart/form-data 参数类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "title", value = "消息标题", required = true, dataType = "String", paramType = "query", example = "系统通知"),
            @ApiImplicitParam(name = "content", value = "消息内容", required = true, dataType = "String", paramType = "query", example = "这是一条测试消息"),
            @ApiImplicitParam(name = "receiverId", value = "接收者ID", required = true, dataType = "Long", paramType = "query", example = "10001"),
            @ApiImplicitParam(name = "priority", value = "优先级", required = false, dataType = "String", paramType = "query", example = "NORMAL")
    })
    @PostMapping(value = "/form-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> formData(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Long receiverId,
            @RequestParam(required = false, defaultValue = "NORMAL") String priority
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("content", content);
        data.put("receiverId", receiverId);
        data.put("priority", priority);
        data.put("contentType", "multipart/form-data");
        return Result.success("Form-Data 消息发送成功", data);
    }

    /**
     * 2. x-www-form-urlencoded 请求
     */
    @ApiOperation(value = "URL 编码查询消息", notes = "演示 application/x-www-form-urlencoded 参数类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Long", paramType = "query", example = "10001"),
            @ApiImplicitParam(name = "status", value = "消息状态", required = false, dataType = "String", paramType = "query", example = "UNREAD"),
            @ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "Integer", paramType = "query", example = "1"),
            @ApiImplicitParam(name = "pageSize", value = "每页大小", required = false, dataType = "Integer", paramType = "query", example = "10")
    })
    @PostMapping(value = "/urlencoded", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<Map<String, Object>> urlencoded(
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("status", status);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        data.put("contentType", "application/x-www-form-urlencoded");
        return Result.success("URL 编码查询成功", data);
    }

    /**
     * 3. JSON 请求
     */
    @ApiOperation(value = "JSON 发送消息", notes = "演示 application/json 参数类型")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<MessageJsonRequest> json(
            @ApiParam(value = "消息 JSON 数据", required = true)
            @RequestBody MessageJsonRequest request
    ) {
        return Result.success("JSON 消息发送成功", request);
    }

    /**
     * 4. XML 请求
     */
    @ApiOperation(value = "XML 发送消息", notes = "演示 application/xml 参数类型")
    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<MessageXmlRequest> xml(
            @ApiParam(value = "消息 XML 数据", required = true)
            @RequestBody MessageXmlRequest request
    ) {
        return Result.success("XML 消息发送成功", request);
    }

    /**
     * 5. Text 请求
     */
    @ApiOperation(value = "Text 消息模板", notes = "演示 text/plain 参数类型，用于发送纯文本消息模板")
    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Result<Map<String, Object>> text(
            @ApiParam(value = "纯文本模板内容，例如：尊敬的用户，您的验证码是：{code}，有效期5分钟", required = true)
            @RequestBody String template
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("template", template);
        data.put("length", template != null ? template.length() : 0);
        data.put("contentType", "text/plain");
        return Result.success("Text 模板保存成功", data);
    }

    /**
     * 6. Binary 文件上传请求
     */
    @ApiOperation(value = "Binary 附件上传", notes = "演示 multipart/form-data 文件上传参数类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "附件文件", required = true, dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "messageId", value = "关联消息ID", required = true, dataType = "String", paramType = "query", example = "MSG001"),
            @ApiImplicitParam(name = "attachmentType", value = "附件类型", required = true, dataType = "String", paramType = "query", example = "image"),
            @ApiImplicitParam(name = "description", value = "附件描述", required = false, dataType = "String", paramType = "query", example = "图片附件")
    })
    @PostMapping(value = "/binary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> binary(
            @RequestParam("file") MultipartFile file,
            @RequestParam String messageId,
            @RequestParam String attachmentType,
            @RequestParam(required = false) String description
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("fileName", file.getOriginalFilename());
        data.put("fileSize", file.getSize());
        data.put("contentType", file.getContentType());
        data.put("messageId", messageId);
        data.put("attachmentType", attachmentType);
        data.put("description", description);
        return Result.success("Binary 附件上传成功", data);
    }
}

