package com.dispatchsim;

import com.dispatchsim.Support.IntegrationTestDataHelper;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.infrastructure.websocket.RealtimeUpdatePublisher;
import com.dispatchsim.service.OrderService;
import com.dispatchsim.service.SimulationEngine;
import com.dispatchsim.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Manual performance suite")
class DispatchSimPerformanceTests {

    @Autowired
    private IntegrationTestDataHelper dataHelper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private RealtimeUpdatePublisher realtimeUpdatePublisher;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @SpyBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        dataHelper.resetState();
    }

    @Test
    void shouldHandleHundredVehiclesAndThousandOrdersUnderLoad() {
        dataHelper.reseedVehicles(100);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1_000; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    new PositionDto(10 + (i % 20), 10 + (i % 10)),
                    new PositionDto(20 + (i % 20), 20 + (i % 10)),
                    5
            ));
        }
        long elapsed = System.currentTimeMillis() - start;
        assertThat(orderRepository.count()).isEqualTo(1_000);
        assertThat(elapsed).isLessThan(60_000);
    }

    @Test
    void shouldReturnStatisticsAndQueriesQuicklyWithWarmData() {
        dataHelper.reseedVehicles(100);
        for (int i = 0; i < 1_000; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    new PositionDto(10 + (i % 20), 10 + (i % 10)),
                    new PositionDto(20 + (i % 20), 20 + (i % 10)),
                    5
            ));
        }

        long start = System.nanoTime();
        statisticsService.getOverview();
        statisticsService.getOrderStatistics();
        statisticsService.getVehicleStatistics();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(5_000);
    }

    @Test
    void shouldAdvanceSimulationTicksWithinAcceptableTime() {
        dataHelper.reseedVehicles(100);
        for (int i = 0; i < 300; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    new PositionDto(10 + (i % 20), 10 + (i % 10)),
                    new PositionDto(12 + (i % 20), 12 + (i % 10)),
                    5
            ));
        }

        simulationEngine.start();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            simulationEngine.tick();
        }
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(20_000);
    }

    @Test
    void shouldPublishRealtimeUpdatesWithinAcceptableLatency() {
        dataHelper.reseedVehicles(100);
        for (int i = 0; i < 100; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    new PositionDto(10 + (i % 20), 10 + (i % 10)),
                    new PositionDto(12 + (i % 20), 12 + (i % 10)),
                    5
            ));
        }

        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        long start = System.nanoTime();
        realtimeUpdatePublisher.publishVehiclePositions(vehicles, orders);
        realtimeUpdatePublisher.publishOrderStatuses(orders);
        realtimeUpdatePublisher.publishVehicleStatuses(vehicles);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/vehicle/position"), any(Object.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/order/status"), any(Object.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/vehicle/status"), any(Object.class));
        assertThat(elapsedMs).isLessThan(2_000);
    }
}
