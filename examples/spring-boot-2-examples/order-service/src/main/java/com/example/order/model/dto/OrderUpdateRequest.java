package com.example.order.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新订单请求 DTO
 */
@Schema(description = "更新订单请求")
public class OrderUpdateRequest {

    @Schema(description = "收货地址", required = false, example = "北京市海淀区xxx街道")
    private String shippingAddress;

    @Schema(description = "收货人姓名", required = false, example = "李四")
    private String receiverName;

    @Schema(description = "收货人电话", required = false, example = "13900139000")
    private String receiverPhone;

    @Schema(description = "订单备注", required = false, example = "修改后的备注")
    private String remark;

    // Getters and Setters
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

