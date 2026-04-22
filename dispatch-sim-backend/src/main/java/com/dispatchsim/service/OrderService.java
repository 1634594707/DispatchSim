package com.dispatchsim.service;

import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.dto.order.CancelOrderRequest;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.order.OrderDto;

import java.util.List;

public interface OrderService {

    OrderDto createOrder(CreateOrderRequest request);

    List<OrderDto> createOrdersBatch(List<CreateOrderRequest> requests);

    List<OrderDto> listOrders(OrderStatus status);

    OrderDto getOrder(Long id);

    OrderDto cancelOrder(Long id, CancelOrderRequest request);

    OrderDto markDeliveryStarted(Long id);

    OrderDto markCompleted(Long id);

    OrderDto rollbackToPending(Long id);
}
