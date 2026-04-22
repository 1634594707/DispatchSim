package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.order.CancelOrderRequest;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ORDERS)
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "创建、查询与取消配送订单")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单", description = "创建新订单并尝试立即触发调度分配")
    public ApiResponse<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @GetMapping
    @Operation(summary = "查询订单列表", description = "按状态筛选订单；不传状态时返回全部订单")
    public ApiResponse<List<OrderDto>> listOrders(@RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(orderService.listOrders(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情")
    public ApiResponse<OrderDto> getOrder(@Parameter(description = "订单 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(orderService.getOrder(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消未完成订单，并释放已占用车辆")
    public ApiResponse<OrderDto> cancelOrder(
            @Parameter(description = "订单 ID", example = "1") @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request) {
        return ApiResponse.success(orderService.cancelOrder(id, request == null ? new CancelOrderRequest(null) : request));
    }
}
