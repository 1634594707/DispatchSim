package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.StateException;
import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.dto.order.ArchiveOrderRequest;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.support.DomainDtoMapper;
import com.dispatchsim.service.ArchiveService;
import com.dispatchsim.service.ReplayService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private static final List<OrderStatus> AUTO_ARCHIVE_STATUSES = List.of(OrderStatus.COMPLETED);

    private final OrderRepository orderRepository;
    private final DomainDtoMapper mapper;
    private final ReplayService replayService;
    private final RealtimeStateCoordinator realtimeStateCoordinator;

    @Override
    @Transactional
    public OrderDto archiveOrder(Long orderId, ArchiveOrderRequest request) {
        Order order = findOrder(orderId);
        try {
            order.archive(request == null ? null : request.reason());
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        replayService.recordOrderStatusChange(saved);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), true);
        log.info("Order {} archived", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public OrderDto restoreOrder(Long orderId) {
        Order order = findOrder(orderId);
        try {
            order.restore();
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Order saved = orderRepository.save(order);
        replayService.recordOrderStatusChange(saved);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(saved), true);
        log.info("Order {} restored from archive", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    public PageResponse<OrderDto> listArchivedOrders(Instant archivedFrom, Instant archivedTo, String reason, String orderNo, int page, int size) {
        return PageResponse.from(
                orderRepository.searchArchived(
                                archivedFrom,
                                archivedTo,
                                normalize(reason),
                                normalize(orderNo),
                                PageRequest.of(page, size)
                        )
                        .map(mapper::toDto)
        );
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 3600000)
    public void autoArchiveCompletedOrders() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Order> orders = orderRepository.findByArchivedFalseAndStatusInAndCompletedAtBefore(AUTO_ARCHIVE_STATUSES, cutoff);
        if (orders.isEmpty()) {
            return;
        }

        orders.forEach(order -> order.archive("系统自动归档"));
        List<Order> savedOrders = orderRepository.saveAll(orders);
        savedOrders.forEach(replayService::recordOrderStatusChange);
        realtimeStateCoordinator.recordAndPublishOrders(savedOrders, true);
        log.info("Auto archived {} completed orders", savedOrders.size());
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
