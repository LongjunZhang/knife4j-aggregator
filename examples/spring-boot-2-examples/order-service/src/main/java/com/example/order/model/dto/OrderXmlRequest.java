package com.example.order.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 订单 XML 请求
 */
@Schema(description = "订单 XML 请求")
@JacksonXmlRootElement(localName = "orderRequest")
public class OrderXmlRequest {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100001")
    @JacksonXmlProperty(localName = "orderId")
    private Long orderId;

    @Schema(description = "客户姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @JacksonXmlProperty(localName = "customerName")
    private String customerName;

    @Schema(description = "商品SKU", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "SKU-001")
    @JacksonXmlProperty(localName = "sku")
    private String sku;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "99.99")
    @JacksonXmlProperty(localName = "price")
    private BigDecimal price;

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

