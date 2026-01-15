package com.example.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单视图对象
 */
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID", example = "202401150001")
    private String orderId;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "订单状态（PENDING-待付款，PAID-已付款，SHIPPED-已发货，COMPLETED-已完成，CANCELLED-已取消）", example = "PENDING")
    private String status;

    @Schema(description = "订单总金额", example = "13998.00")
    private BigDecimal totalAmount;

    @Schema(description = "收货地址", example = "北京市朝阳区xxx街道")
    private String shippingAddress;

    @Schema(description = "收货人姓名", example = "张三")
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800138000")
    private String receiverPhone;

    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

    @Schema(description = "订单商品列表")
    private List<OrderItemVO> items;

    @Schema(description = "创建时间", example = "2024-01-15T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-15T15:45:00")
    private LocalDateTime updateTime;

    // 嵌套类：订单项
    @Schema(description = "订单商品项")
    public static class OrderItemVO {
        @Schema(description = "商品ID", example = "2001")
        private Long productId;

        @Schema(description = "商品名称", example = "iPhone 15")
        private String productName;

        @Schema(description = "商品数量", example = "2")
        private Integer quantity;

        @Schema(description = "商品单价", example = "6999.00")
        private BigDecimal unitPrice;

        @Schema(description = "小计金额", example = "13998.00")
        private BigDecimal subtotal;

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

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    // 静态工厂方法
    public static OrderVO mock(String orderId) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(orderId);
        vo.setUserId(1001L);
        vo.setStatus("PENDING");
        vo.setTotalAmount(new BigDecimal("13998.00"));
        vo.setShippingAddress("北京市朝阳区xxx街道");
        vo.setReceiverName("张三");
        vo.setReceiverPhone("13800138000");
        vo.setRemark("请尽快发货");

        List<OrderItemVO> items = new ArrayList<>();
        OrderItemVO item = new OrderItemVO();
        item.setProductId(2001L);
        item.setProductName("iPhone 15");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("6999.00"));
        item.setSubtotal(new BigDecimal("13998.00"));
        items.add(item);
        vo.setItems(items);

        vo.setCreateTime(LocalDateTime.now().minusHours(2));
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public List<OrderItemVO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVO> items) {
        this.items = items;
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

