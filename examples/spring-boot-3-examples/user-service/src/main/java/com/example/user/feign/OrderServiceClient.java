package com.example.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 订单服务 Feign 客户端
 * 用于调用 OrderService 的接口
 */
@FeignClient(name = "order-service")
public interface OrderServiceClient {

    /**
     * 调用 OrderService 的测试接口
     * @return 测试响应字符串
     */
    @GetMapping("/order/test")
    String testConnection();

}

