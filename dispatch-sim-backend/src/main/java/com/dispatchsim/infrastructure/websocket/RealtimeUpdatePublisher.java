package com.dispatchsim.infrastructure.websocket;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.statistics.StatisticsOverviewDto;
import com.dispatchsim.dto.support.DomainDtoMapper;
import com.dispatchsim.dto.vehicle.VehicleDto;
import com.dispatchsim.service.PerformanceMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealtimeUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final DomainDtoMapper mapper;
    private final PerformanceMetricsService performanceMetricsService;

    public void publishVehiclePositions(Collection<Vehicle> vehicles, Collection<Order> orders) {
        List<VehicleDto> vehicleDtos = vehicles.stream().map(mapper::toDto).toList();
        List<OrderDto> orderDtos = orders.stream().map(mapper::toDto).toList();
        messagingTemplate.convertAndSend("/topic/vehicle/position", Map.of(
                "vehicles", vehicleDtos,
                "orders", orderDtos
        ));
        performanceMetricsService.recordWebsocketPublish("/topic/vehicle/position", vehicleDtos.size() + orderDtos.size());
    }

    public void publishOrderStatuses(Collection<Order> orders) {
        List<OrderDto> orderDtos = orders.stream().map(mapper::toDto).toList();
        messagingTemplate.convertAndSend("/topic/order/status", orderDtos);
        performanceMetricsService.recordWebsocketPublish("/topic/order/status", orderDtos.size());
    }

    public void publishVehicleStatuses(Collection<Vehicle> vehicles) {
        List<VehicleDto> vehicleDtos = vehicles.stream().map(mapper::toDto).toList();
        messagingTemplate.convertAndSend("/topic/vehicle/status", vehicleDtos);
        performanceMetricsService.recordWebsocketPublish("/topic/vehicle/status", vehicleDtos.size());
    }

    public void publishStatistics(StatisticsOverviewDto overview) {
        messagingTemplate.convertAndSend("/topic/statistics/realtime", overview);
        performanceMetricsService.recordWebsocketPublish("/topic/statistics/realtime", 1);
    }
}
