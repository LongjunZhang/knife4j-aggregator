package com.example.order.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 订单 JSON 请求
 */
@Schema(description = "订单 JSON 请求")
public class OrderJsonRequest {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORD20240101001")
    private String orderNo;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "iPhone 15 Pro")
    private String productName;

    @Schema(description = "商品数量", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2")
    private Integer quantity;

    @Schema(description = "订单金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "15998.00")
    private BigDecimal amount;

    @Schema(description = "收货地址", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "北京市朝阳区xxx路xxx号")
    private String address;

    // Getters and Setters
    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

