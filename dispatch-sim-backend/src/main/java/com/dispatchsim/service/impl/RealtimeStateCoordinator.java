package com.dispatchsim.service.impl;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.infrastructure.websocket.RealtimeUpdatePublisher;
import com.dispatchsim.service.ReplayService;
import com.dispatchsim.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealtimeStateCoordinator {

    private final ReplayService replayService;
    private final RealtimeUpdatePublisher publisher;
    private final StatisticsService statisticsService;

    public void recordAndPublishOrders(Collection<Order> orders, boolean refreshStatistics) {
        if (orders == null || orders.isEmpty()) {
            if (refreshStatistics) {
                publishStatistics(true);
            }
            return;
        }

        recordOrders(orders);
        publishOrders(orders);
        if (refreshStatistics) {
            publishStatistics(true);
        }
    }

    public void recordAndPublishVehicles(Collection<Vehicle> vehicles, boolean refreshStatistics) {
        if (vehicles == null || vehicles.isEmpty()) {
            if (refreshStatistics) {
                publishStatistics(true);
            }
            return;
        }

        vehicles.forEach(replayService::recordVehicleStatusChange);
        publisher.publishVehicleStatuses(vehicles);
        if (refreshStatistics) {
            publishStatistics(true);
        }
    }

    public void recordOrders(Collection<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        orders.forEach(replayService::recordOrderStatusChange);
    }

    public void publishOrders(Collection<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        publisher.publishOrderStatuses(orders);
    }

    public void publishStatistics(boolean invalidateCaches) {
        if (invalidateCaches) {
            statisticsService.invalidateCaches();
        }
        publisher.publishStatistics(statisticsService.refreshOverview());
    }

    public void recordVehiclePositions(Collection<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }
        vehicles.forEach(replayService::recordVehiclePositionUpdate);
    }

    public void recordVehicleStatuses(Collection<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }
        vehicles.forEach(replayService::recordVehicleStatusChange);
    }

    public void publishVehiclePositions(Collection<Vehicle> vehicles, Collection<Order> orders) {
        publisher.publishVehiclePositions(
                vehicles == null ? List.of() : vehicles,
                orders == null ? List.of() : orders
        );
    }
}
