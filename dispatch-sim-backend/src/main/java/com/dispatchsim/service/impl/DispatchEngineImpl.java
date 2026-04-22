package com.dispatchsim.service.impl;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.event.VehicleAssignedEvent;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.domain.strategy.VehicleSelectionStrategy;
import com.dispatchsim.domain.strategy.VehicleSelectionStrategyRegistry;
import com.dispatchsim.infrastructure.websocket.RealtimeUpdatePublisher;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.DomainEventPublisher;
import com.dispatchsim.service.PerformanceMetricsService;
import com.dispatchsim.service.ReplayService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatchEngineImpl implements DispatchEngine {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleSelectionStrategyRegistry strategyRegistry;
    private final RealtimeUpdatePublisher publisher;
    private final DomainEventPublisher domainEventPublisher;
    private final ReplayService replayService;
    private final PerformanceMetricsService performanceMetricsService;

    private volatile DispatchStrategy currentStrategy = DispatchStrategy.NEAREST_FIRST;
    private final Map<DispatchStrategy, Long> strategyUsageStats = new EnumMap<>(DispatchStrategy.class);

    @Override
    @Transactional
    public void dispatchPendingOrders() {
        long startedAt = System.nanoTime();
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Vehicle> availableVehicles = new ArrayList<>(vehicleRepository.findByStatus(VehicleStatus.IDLE));
        if (pendingOrders.isEmpty() || availableVehicles.isEmpty()) {
            performanceMetricsService.recordDispatchDuration((System.nanoTime() - startedAt) / 1_000_000, 0);
            return;
        }

        log.info("Dispatching {} pending orders with {} available vehicles using strategy {}",
                pendingOrders.size(), availableVehicles.size(), currentStrategy);
        VehicleSelectionStrategy strategy = strategyRegistry.get(currentStrategy);
        List<Order> updatedOrders = new ArrayList<>();
        List<Vehicle> updatedVehicles = new ArrayList<>();

        for (Order order : pendingOrders) {
            if (availableVehicles.isEmpty()) {
                break;
            }

            Vehicle selected = strategy.select(order, availableVehicles).orElse(null);
            if (selected == null) {
                continue;
            }

            order.assignToVehicle(selected.getId(), currentStrategy.name());
            selected.assignOrder(order.getId());

            updatedOrders.add(order);
            updatedVehicles.add(selected);
            availableVehicles.removeIf(vehicle -> vehicle.getId().equals(selected.getId()));
            strategyUsageStats.merge(currentStrategy, 1L, Long::sum);
        }

        if (!updatedOrders.isEmpty()) {
            orderRepository.saveAll(updatedOrders);
            vehicleRepository.saveAll(updatedVehicles);
            updatedOrders.forEach(replayService::recordOrderStatusChange);
            updatedVehicles.forEach(replayService::recordVehicleStatusChange);
            updatedVehicles.forEach(vehicle ->
                    domainEventPublisher.publish("VEHICLE", vehicle.getId(), new VehicleAssignedEvent(vehicle.getId()))
            );
            publisher.publishOrderStatuses(updatedOrders);
            publisher.publishVehicleStatuses(updatedVehicles);
            log.info("Dispatched {} orders", updatedOrders.size());
        }
        performanceMetricsService.recordDispatchDuration((System.nanoTime() - startedAt) / 1_000_000, updatedOrders.size());
    }

    @Override
    public DispatchStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    @Override
    public void setCurrentStrategy(DispatchStrategy strategy) {
        this.currentStrategy = strategy;
    }

    @Override
    public Map<DispatchStrategy, Long> getStrategyUsageStats() {
        return Map.copyOf(strategyUsageStats);
    }
}
