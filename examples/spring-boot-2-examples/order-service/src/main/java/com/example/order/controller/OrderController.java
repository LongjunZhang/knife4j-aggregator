package com.example.order.controller;

import com.example.order.model.dto.OrderCreateRequest;
import com.example.order.model.dto.OrderUpdateRequest;
import com.example.order.model.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单管理接口
 * 提供订单的增删改查操作（GET/POST/PUT/PATCH/DELETE）
 */
@Tag(name = "订单管理", description = "订单 CRUD 操作接口")
@Validated
@RestController
@RequestMapping("/order")
public class OrderController {

    /**
     * 获取订单列表
     */
    @Operation(summary = "获取订单列表", description = "分页查询订单列表")
    @GetMapping("/list")
    public List<OrderVO> list(
            @Parameter(description = "页码（必须大于0）", example = "1")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,

            @Parameter(description = "每页大小（必须大于0）", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer pageSize,

            @Parameter(description = "用户ID", example = "1001")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "订单状态", example = "PENDING")
            @RequestParam(required = false) String status
    ) {
        // 模拟返回数据
        List<OrderVO> orders = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            String orderId = String.format("2024011500%02d", (pageNum - 1) * pageSize + i);
            orders.add(OrderVO.mock(orderId));
        }
        return orders;
    }

    /**
     * 根据订单号获取订单
     */
    @Operation(summary = "获取订单详情", description = "根据订单号获取订单详细信息")
    @GetMapping("/{orderId}")
    public OrderVO getById(
            @Parameter(description = "订单号（不能为空）", required = true, example = "202401150001")
            @PathVariable @NotBlank(message = "订单号不能为空") String orderId
    ) {
        return OrderVO.mock(orderId);
    }

    /**
     * 创建订单
     */
    @Operation(summary = "创建订单", description = "创建新订单")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderVO create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单创建请求体",
                    required = true
            )
            @Valid @RequestBody OrderCreateRequest request
    ) {
        // 模拟创建订单
        String orderId = String.format("2024%010d", System.currentTimeMillis() % 10000000000L);
        OrderVO vo = new OrderVO();
        vo.setOrderId(orderId);
        vo.setUserId(request.getUserId());
        vo.setStatus("PENDING");
        vo.setShippingAddress(request.getShippingAddress());
        vo.setReceiverName(request.getReceiverName());
        vo.setReceiverPhone(request.getReceiverPhone());
        vo.setRemark(request.getRemark());
        
        // 计算总金额
        BigDecimal total = BigDecimal.ZERO;
        List<OrderVO.OrderItemVO> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (OrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
                OrderVO.OrderItemVO item = new OrderVO.OrderItemVO();
                item.setProductId(itemReq.getProductId());
                item.setProductName(itemReq.getProductName());
                item.setQuantity(itemReq.getQuantity());
                item.setUnitPrice(itemReq.getUnitPrice());
                BigDecimal subtotal = itemReq.getUnitPrice().multiply(new BigDecimal(itemReq.getQuantity()));
                item.setSubtotal(subtotal);
                total = total.add(subtotal);
                items.add(item);
            }
        }
        vo.setItems(items);
        vo.setTotalAmount(total);
        return vo;
    }

    /**
     * 更新订单（全量）
     */
    @Operation(summary = "更新订单（全量）", description = "根据订单号更新订单信息")
    @PutMapping("/{orderId}")
    public OrderVO update(
            @Parameter(description = "订单号", required = true, example = "202401150001")
            @PathVariable @NotBlank(message = "订单号不能为空") String orderId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单更新请求体",
                    required = true
            )
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        // 模拟更新订单
        OrderVO vo = OrderVO.mock(orderId);
        if (request.getShippingAddress() != null) vo.setShippingAddress(request.getShippingAddress());
        if (request.getReceiverName() != null) vo.setReceiverName(request.getReceiverName());
        if (request.getReceiverPhone() != null) vo.setReceiverPhone(request.getReceiverPhone());
        if (request.getRemark() != null) vo.setRemark(request.getRemark());
        return vo;
    }

    /**
     * 更新订单状态
     */
    @Operation(summary = "更新订单状态", description = "根据订单号更新订单状态")
    @PatchMapping("/{orderId}/status")
    public OrderVO updateStatus(
            @Parameter(description = "订单号", required = true, example = "202401150001")
            @PathVariable @NotBlank(message = "订单号不能为空") String orderId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "状态更新请求体",
                    required = true
            )
            @RequestBody Map<String, String> statusUpdate
    ) {
        // 模拟更新订单状态
        OrderVO vo = OrderVO.mock(orderId);
        if (statusUpdate.containsKey("status")) {
            vo.setStatus(statusUpdate.get("status"));
        }
        return vo;
    }

    /**
     * 取消/删除订单
     */
    @Operation(summary = "取消订单", description = "根据订单号取消/删除订单")
    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "订单号", required = true, example = "202401150001")
            @PathVariable @NotBlank(message = "订单号不能为空") String orderId
    ) {
        // 模拟取消订单
        // 实际业务中执行取消/删除逻辑
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
