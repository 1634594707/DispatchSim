package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.StateException;
import com.dispatchsim.domain.event.OrderCancelledEvent;
import com.dispatchsim.domain.event.OrderCompletedEvent;
import com.dispatchsim.domain.event.OrderCreatedEvent;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.dto.order.ArchiveOrderRequest;
import com.dispatchsim.dto.order.CancelOrderRequest;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.depot.DepotDto;
import com.dispatchsim.dto.support.DomainDtoMapper;
import com.dispatchsim.service.ArchiveService;
import com.dispatchsim.service.DepotService;
import com.dispatchsim.service.DomainEventPublisher;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.OrderService;
import com.dispatchsim.service.ReplayService;
import com.dispatchsim.service.VehicleService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DomainDtoMapper mapper;
    private final DispatchEngine dispatchEngine;
    private final VehicleService vehicleService;
    private final DomainEventPublisher domainEventPublisher;
    private final ReplayService replayService;
    private final RealtimeStateCoordinator realtimeStateCoordinator;
    private final ArchiveService archiveService;
    private final DepotService depotService;

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        log.info("Creating order from ({}, {}) to ({}, {}) with priority {}",
                request.pickup().x(),
                request.pickup().y(),
                request.delivery().x(),
                request.delivery().y(),
                request.priority());
        Order order = buildOrder(request);
        Order saved = orderRepository.save(order);
        replayService.recordOrderStatusChange(saved);
        domainEventPublisher.publish("ORDER", saved.getId(), new OrderCreatedEvent(saved.getId()));
        dispatchEngine.dispatchPendingOrders();
        Order updated = orderRepository.findById(saved.getId()).orElse(saved);
        replayService.recordOrderStatusChange(updated);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(updated), true);
        log.info("Order {} created and current status is {}", updated.getId(), updated.getStatus());
        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public List<OrderDto> createOrdersBatch(List<CreateOrderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        log.info("Creating {} orders in batch", requests.size());

        List<Order> orders = requests.stream()
                .map(this::buildOrder)
                .toList();
        List<Order> savedOrders = orderRepository.saveAll(orders);
        savedOrders.forEach(order -> {
            replayService.recordOrderStatusChange(order);
            domainEventPublisher.publish("ORDER", order.getId(), new OrderCreatedEvent(order.getId()));
        });

        dispatchEngine.dispatchPendingOrders();

        Map<Long, Order> updatedOrderMap = orderRepository.findAllById(
                        savedOrders.stream().map(Order::getId).toList()
                ).stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, Function.identity()));

        List<Order> updatedOrders = savedOrders.stream()
                .map(order -> updatedOrderMap.getOrDefault(order.getId(), order))
                .sorted(Comparator.comparing(Order::getId))
                .toList();

        updatedOrders.forEach(replayService::recordOrderStatusChange);
        realtimeStateCoordinator.recordAndPublishOrders(updatedOrders, true);

        log.info("Batch created {} orders", updatedOrders.size());
        return updatedOrders.stream().map(mapper::toDto).toList();
    }

    @Override
    public List<OrderDto> listOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findByArchivedFalseOrderByCreatedAtDesc()
                : orderRepository.findByStatusAndArchivedFalseOrderByCreatedAtDesc(status);
        return orders.stream().map(mapper::toDto).toList();
    }

    @Override
    public OrderDto getOrder(Long id) {
        return mapper.toDto(findOrder(id));
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long id, CancelOrderRequest request) {
        log.info("Cancelling order {}", id);
        Order order = findOrder(id);
        Long assignedVehicleId = order.getAssignedVehicleId();
        try {
            order.cancel(request.reason());
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        replayService.recordOrderStatusChange(saved);
        domainEventPublisher.publish("ORDER", saved.getId(), new OrderCancelledEvent(saved.getId()));
        if (assignedVehicleId != null) {
            vehicleService.recoverVehicle(assignedVehicleId);
        }
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), true);
        log.info("Order {} cancelled", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public OrderDto archiveOrder(Long id, ArchiveOrderRequest request) {
        return archiveService.archiveOrder(id, request);
    }

    @Override
    @Transactional
    public OrderDto restoreOrder(Long id) {
        return archiveService.restoreOrder(id);
    }

    @Override
    public PageResponse<OrderDto> listArchivedOrders(Instant archivedFrom, Instant archivedTo, String reason, String orderNo, int page, int size) {
        return archiveService.listArchivedOrders(archivedFrom, archivedTo, reason, orderNo, page, size);
    }

    @Override
    @Transactional
    public OrderDto markDeliveryStarted(Long id) {
        Order order = findOrder(id);
        try {
            order.startDelivery();
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), false);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public OrderDto markCompleted(Long id) {
        log.info("Completing order {}", id);
        Order order = findOrder(id);
        try {
            order.complete();
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        domainEventPublisher.publish("ORDER", saved.getId(), new OrderCompletedEvent(saved.getId()));
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), true);
        log.info("Order {} completed", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public OrderDto rollbackToPending(Long id) {
        Order order = findOrder(id);
        try {
            order.rollbackToPending();
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), false);
        return mapper.toDto(saved);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));
    }

    private Order buildOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pickup(mapper.toEntity(request.pickup()))
                .delivery(mapper.toEntity(request.delivery()))
                .priority(request.priority())
                .depotId(resolveDepotId(request))
                .createdAt(Instant.now())
                .archived(false)
                .dispatchAttempts(0)
                .build();
        order.markPending();
        return order;
    }

    private Long resolveDepotId(CreateOrderRequest request) {
        DepotDto nearestDepot = depotService.findNearestDepot(request.pickup().x(), request.pickup().y());
        return nearestDepot == null ? null : nearestDepot.id();
    }
}
