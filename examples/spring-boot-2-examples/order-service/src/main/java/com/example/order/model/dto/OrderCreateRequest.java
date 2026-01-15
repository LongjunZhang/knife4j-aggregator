package com.example.order.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Schema(description = "创建订单请求")
public class OrderCreateRequest {

    @Schema(description = "用户ID", required = true, example = "1001")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "商品列表", required = true)
    @NotNull(message = "商品列表不能为空")
    private List<OrderItemRequest> items;

    @Schema(description = "收货地址", required = true, example = "北京市朝阳区xxx街道")
    @NotBlank(message = "收货地址不能为空")
    private String shippingAddress;

    @Schema(description = "收货人姓名", required = true, example = "张三")
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @Schema(description = "收货人电话", required = true, example = "13800138000")
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    @Schema(description = "订单备注", required = false, example = "请尽快发货")
    private String remark;

    // 嵌套类：订单项
    @Schema(description = "订单商品项")
    public static class OrderItemRequest {
        @Schema(description = "商品ID", required = true, example = "2001")
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @Schema(description = "商品名称", required = true, example = "iPhone 15")
        @NotBlank(message = "商品名称不能为空")
        private String productName;

        @Schema(description = "商品数量", required = true, example = "2")
        @NotNull(message = "商品数量不能为空")
        private Integer quantity;

        @Schema(description = "商品单价", required = true, example = "6999.00")
        @NotNull(message = "商品单价不能为空")
        @DecimalMin(value = "0.01", message = "商品单价必须大于0")
        private BigDecimal unitPrice;

        // Getters and Setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
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

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

