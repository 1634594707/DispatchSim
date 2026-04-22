package com.dispatchsim.Support;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.OutboxRepository;
import com.dispatchsim.domain.repository.ReplayEventLogRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.SimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class IntegrationTestDataHelper {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final OutboxRepository outboxRepository;
    private final ReplayEventLogRepository replayEventLogRepository;
    private final SimulationEngine simulationEngine;
    private final DispatchEngine dispatchEngine;

    public void resetState() {
        simulationEngine.stop();
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
        replayEventLogRepository.deleteAll();
        vehicleRepository.deleteAll();
        dispatchEngine.setCurrentStrategy(DispatchStrategy.NEAREST_FIRST);
        seedVehicles(10);
    }

    public void reseedVehicles(int count) {
        vehicleRepository.deleteAll();
        seedVehicles(count);
    }

    private void seedVehicles(int count) {
        List<Vehicle> vehicles = IntStream.range(0, count)
                .mapToObj(index -> Vehicle.builder()
                        .status(VehicleStatus.IDLE)
                        .currentPosition(Position.builder()
                                .x(10 + (index % 5) * 25)
                                .y(10 + (index / 5) * 35)
                                .build())
                        .battery(80 + (index % 3) * 5)
                        .speed(5.0)
                        .maxSpeed(8.0)
                        .currentLoad(0.0)
                        .capacity(50.0)
                        .heading(0.0)
                        .totalTasks(0)
                        .totalDistance(0.0)
                        .build())
                .toList();

        vehicleRepository.saveAll(vehicles);
    }
}
