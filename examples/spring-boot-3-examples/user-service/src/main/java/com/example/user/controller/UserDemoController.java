package com.example.user.controller;

import com.example.user.model.dto.UserJsonRequest;
import com.example.user.model.dto.UserXmlRequest;
import com.example.user.model.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户演示接口
 * 演示不同请求参数类型：form-data, x-www-form-urlencoded, JSON, XML, Text, Binary
 */
// @Tag(name = "用户演示接口", description = "演示不同请求参数类型（OpenAPI 3 风格）")
// @RestController
// @RequestMapping("/user/demo")
public class UserDemoController {

    /**
     * 1. form-data 请求
     */
    @Operation(summary = "Form-Data 请求", description = "演示 multipart/form-data 参数类型")
    @PostMapping(value = "/form-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> formData(
            @Parameter(description = "用户名", required = true, example = "zhangsan")
            @RequestParam String username,

            @Parameter(description = "年龄", required = false, example = "28")
            @RequestParam(required = false) Integer age,

            @Parameter(description = "邮箱地址", required = true, example = "zhangsan@test.com")
            @RequestParam String email,

            @Parameter(description = "备注信息", required = false, example = "这是备注")
            @RequestParam(required = false) String remark
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("age", age);
        data.put("email", email);
        data.put("remark", remark);
        data.put("contentType", "multipart/form-data");
        return Result.success("Form-Data 请求成功", data);
    }

    /**
     * 2. x-www-form-urlencoded 请求
     */
    @Operation(summary = "URL 编码请求", description = "演示 application/x-www-form-urlencoded 参数类型")
    @PostMapping(value = "/urlencoded", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<Map<String, Object>> urlencoded(
            @Parameter(description = "账号", required = true, example = "admin")
            @RequestParam String account,

            @Parameter(description = "密码", required = true, example = "123456")
            @RequestParam String password,

            @Parameter(description = "记住我", required = false, example = "true")
            @RequestParam(required = false, defaultValue = "false") Boolean rememberMe,

            @Parameter(description = "验证码", required = false, example = "ABCD")
            @RequestParam(required = false) String captcha
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("password", "******");
        data.put("rememberMe", rememberMe);
        data.put("captcha", captcha);
        data.put("contentType", "application/x-www-form-urlencoded");
        return Result.success("URL 编码请求成功", data);
    }

    /**
     * 3. JSON 请求
     */
    @Operation(summary = "JSON 请求", description = "演示 application/json 参数类型")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserJsonRequest> json(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户 JSON 数据",
                    required = true
            )
            @RequestBody UserJsonRequest request
    ) {
        return Result.success("JSON 请求成功", request);
    }

    /**
     * 4. XML 请求
     */
    @Operation(summary = "XML 请求", description = "演示 application/xml 参数类型")
    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserXmlRequest> xml(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户 XML 数据",
                    required = true
            )
            @RequestBody UserXmlRequest request
    ) {
        return Result.success("XML 请求成功", request);
    }

    /**
     * 5. Text 请求
     */
    @Operation(summary = "Text 请求", description = "演示 text/plain 参数类型，可上传纯文本内容")
    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Result<Map<String, Object>> text(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "纯文本内容，例如：这是一段测试文本",
                    required = true
            )
            @RequestBody String textContent
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", textContent);
        data.put("length", textContent != null ? textContent.length() : 0);
        data.put("contentType", "text/plain");
        return Result.success("Text 请求成功", data);
    }

    /**
     * 6. Binary 文件上传请求
     */
    @Operation(summary = "Binary 文件上传", description = "演示 multipart/form-data 文件上传参数类型")
    @PostMapping(value = "/binary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> binary(
            @Parameter(description = "上传的文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "文件描述", required = false, example = "测试文件")
            @RequestParam(required = false) String description,

            @Parameter(description = "文件分类", required = true, example = "document")
            @RequestParam String category,

            @Parameter(description = "是否公开", required = false, example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean isPublic
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("fileName", file.getOriginalFilename());
        data.put("fileSize", file.getSize());
        data.put("contentType", file.getContentType());
        data.put("description", description);
        data.put("category", category);
        data.put("isPublic", isPublic);
        return Result.success("Binary 文件上传成功", data);
    }
}

