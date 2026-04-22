package com.dispatchsim;

import com.dispatchsim.Support.IntegrationTestDataHelper;
import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.order.CancelOrderRequest;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.simulation.SimulationStatusDto;
import com.dispatchsim.dto.vehicle.FaultRequest;
import com.dispatchsim.service.OrderService;
import com.dispatchsim.service.SimulationEngine;
import com.dispatchsim.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class DispatchSimWorkflowIntegrationTests {

    @Autowired
    private IntegrationTestDataHelper dataHelper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private SimulationEngine simulationEngine;

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
    void shouldCompleteEndToEndOrderLifecycleAndPushRealtimeTopics() {
        OrderDto created = orderService.createOrder(new CreateOrderRequest(
                new PositionDto(10, 10),
                new PositionDto(12, 12),
                5
        ));

        assertThat(created.status()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(created.assignedVehicleId()).isNotNull();

        SimulationStatusDto started = simulationEngine.start();
        assertThat(started.status().name()).isEqualTo("RUNNING");

        for (int i = 0; i < 5; i++) {
            simulationEngine.tick();
        }

        Order completed = orderRepository.findById(created.id()).orElseThrow();
        Vehicle vehicle = vehicleRepository.findById(completed.getAssignedVehicleId()).orElseThrow();

        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IDLE);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(ArgumentMatchers.eq("/topic/order/status"), ArgumentMatchers.<Object>any());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(ArgumentMatchers.eq("/topic/vehicle/position"), ArgumentMatchers.<Object>any());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(ArgumentMatchers.eq("/topic/vehicle/status"), ArgumentMatchers.<Object>any());
    }

    @Test
    void shouldRedispatchAfterVehicleFaultAndAllowStrategySwitchAndCancellation() {
        simulationEngine.updateStrategy(DispatchStrategy.FASTEST_ARRIVAL);
        assertThat(simulationEngine.getStatus().strategy()).isEqualTo(DispatchStrategy.FASTEST_ARRIVAL);

        OrderDto created = orderService.createOrder(new CreateOrderRequest(
                new PositionDto(10, 10),
                new PositionDto(35, 10),
                7
        ));

        Long originalVehicleId = created.assignedVehicleId();
        assertThat(originalVehicleId).isNotNull();

        vehicleService.faultVehicle(originalVehicleId, new FaultRequest("integration-test"));

        Order reassigned = orderRepository.findById(created.id()).orElseThrow();
        assertThat(reassigned.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(reassigned.getAssignedVehicleId()).isNotNull();
        assertThat(reassigned.getAssignedVehicleId()).isNotEqualTo(originalVehicleId);

        orderService.cancelOrder(reassigned.getId(), new CancelOrderRequest("integration-test cancel"));
        Order cancelled = orderRepository.findById(reassigned.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(ArgumentMatchers.eq("/topic/order/status"), ArgumentMatchers.<Object>any());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(ArgumentMatchers.eq("/topic/vehicle/status"), ArgumentMatchers.<Object>any());
    }

    @Test
    void shouldProcessBatchOrdersAcrossMultipleVehicles() {
        for (int i = 0; i < 20; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    new PositionDto(10 + (i % 5), 10 + (i % 4)),
                    new PositionDto(12 + (i % 5), 12 + (i % 4)),
                    5 + (i % 3)
            ));
        }

        simulationEngine.start();
        for (int i = 0; i < 30; i++) {
            simulationEngine.tick();
        }

        List<Order> orders = orderRepository.findAll();
        List<Vehicle> vehicles = vehicleRepository.findAll();

        assertThat(orders).hasSize(20);
        assertThat(orders)
                .extracting(Order::getStatus)
                .containsOnly(OrderStatus.COMPLETED);
        assertThat(orders)
                .allSatisfy(order -> {
                    assertThat(order.getAssignedVehicleId()).isNotNull();
                    assertThat(order.getStrategy()).isNotBlank();
                });

        assertThat(vehicles)
                .filteredOn(vehicle -> vehicle.getTotalTasks() != null && vehicle.getTotalTasks() > 0)
                .hasSizeGreaterThan(1);
        assertThat(vehicles)
                .extracting(Vehicle::getStatus)
                .containsOnly(VehicleStatus.IDLE);
    }
}
