package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.dto.order.ArchiveOrderRequest;
import com.dispatchsim.dto.order.CancelOrderRequest;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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

    @GetMapping("/archived")
    @Operation(summary = "查询归档订单", description = "支持按归档时间、归档原因和订单号筛选")
    public ApiResponse<PageResponse<OrderDto>> listArchivedOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant archivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant archivedTo,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listArchivedOrders(archivedFrom, archivedTo, reason, orderNo, page, size));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档订单", description = "仅允许归档已完成或已取消订单")
    public ApiResponse<OrderDto> archiveOrder(
            @Parameter(description = "订单 ID", example = "1") @PathVariable Long id,
            @RequestBody(required = false) ArchiveOrderRequest request) {
        return ApiResponse.success(orderService.archiveOrder(id, request == null ? new ArchiveOrderRequest(null) : request));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "恢复归档订单", description = "将订单从归档列表恢复到活动列表")
    public ApiResponse<OrderDto> restoreOrder(
            @Parameter(description = "订单 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(orderService.restoreOrder(id));
    }
}
