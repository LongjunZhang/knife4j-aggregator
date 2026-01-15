package com.example.order.controller;

import com.example.order.model.dto.OrderJsonRequest;
import com.example.order.model.dto.OrderXmlRequest;
import com.example.order.model.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单演示接口
 * 演示不同请求参数类型：form-data, x-www-form-urlencoded, JSON, XML, Text, Binary
 */
// @Tag(name = "订单演示接口", description = "演示不同请求参数类型（Spring Boot 2.7.x + springdoc-openapi-ui）")
// @RestController
// @RequestMapping("/order/demo")
public class OrderDemoController {

    /**
     * 1. form-data 请求
     */
    @Operation(summary = "Form-Data 创建订单", description = "演示 multipart/form-data 参数类型")
    @PostMapping(value = "/form-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> formData(
            @Parameter(description = "订单编号", required = true, example = "ORD20240101001")
            @RequestParam String orderNo,

            @Parameter(description = "商品ID", required = true, example = "PROD001")
            @RequestParam String productId,

            @Parameter(description = "购买数量", required = false, example = "3")
            @RequestParam(required = false, defaultValue = "1") Integer quantity,

            @Parameter(description = "订单金额", required = true, example = "299.99")
            @RequestParam BigDecimal amount
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("productId", productId);
        data.put("quantity", quantity);
        data.put("amount", amount);
        data.put("contentType", "multipart/form-data");
        return Result.success("Form-Data 订单创建成功", data);
    }

    /**
     * 2. x-www-form-urlencoded 请求
     */
    @Operation(summary = "URL 编码查询订单", description = "演示 application/x-www-form-urlencoded 参数类型")
    @PostMapping(value = "/urlencoded", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<Map<String, Object>> urlencoded(
            @Parameter(description = "订单ID", required = true, example = "100001")
            @RequestParam Long orderId,

            @Parameter(description = "订单状态", required = false, example = "PAID")
            @RequestParam(required = false) String status,

            @Parameter(description = "是否包含详情", required = false, example = "true")
            @RequestParam(required = false, defaultValue = "false") Boolean includeDetails,

            @Parameter(description = "排序字段", required = true, example = "createTime")
            @RequestParam String sortBy
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status", status);
        data.put("includeDetails", includeDetails);
        data.put("sortBy", sortBy);
        data.put("contentType", "application/x-www-form-urlencoded");
        return Result.success("URL 编码查询成功", data);
    }

    /**
     * 3. JSON 请求
     */
    @Operation(summary = "JSON 创建订单", description = "演示 application/json 参数类型")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrderJsonRequest> json(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单 JSON 数据",
                    required = true
            )
            @RequestBody OrderJsonRequest request
    ) {
        return Result.success("JSON 订单创建成功", request);
    }

    /**
     * 4. XML 请求
     */
    @Operation(summary = "XML 创建订单", description = "演示 application/xml 参数类型")
    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrderXmlRequest> xml(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单 XML 数据",
                    required = true
            )
            @RequestBody OrderXmlRequest request
    ) {
        return Result.success("XML 订单创建成功", request);
    }

    /**
     * 5. Text 请求
     */
    @Operation(summary = "Text 备注更新", description = "演示 text/plain 参数类型，用于更新订单备注")
    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Result<Map<String, Object>> text(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单备注内容，例如：请尽快发货，易碎品轻拿轻放",
                    required = true
            )
            @RequestBody String remark
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("remark", remark);
        data.put("length", remark != null ? remark.length() : 0);
        data.put("contentType", "text/plain");
        return Result.success("Text 备注更新成功", data);
    }

    /**
     * 6. Binary 文件上传请求
     */
    @Operation(summary = "Binary 订单附件上传", description = "演示 multipart/form-data 文件上传参数类型")
    @PostMapping(value = "/binary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> binary(
            @Parameter(description = "订单附件文件（发票、合同等）", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "关联订单ID", required = true, example = "100001")
            @RequestParam Long orderId,

            @Parameter(description = "附件类型", required = true, example = "invoice")
            @RequestParam String attachmentType,

            @Parameter(description = "附件备注", required = false, example = "电子发票")
            @RequestParam(required = false) String note
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("fileName", file.getOriginalFilename());
        data.put("fileSize", file.getSize());
        data.put("contentType", file.getContentType());
        data.put("orderId", orderId);
        data.put("attachmentType", attachmentType);
        data.put("note", note);
        return Result.success("Binary 订单附件上传成功", data);
    }
}

