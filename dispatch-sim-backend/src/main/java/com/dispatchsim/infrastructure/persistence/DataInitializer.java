package com.dispatchsim.infrastructure.persistence;

import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        if (vehicleRepository.count() > 0) {
            return;
        }

        List<Vehicle> vehicles = IntStream.range(0, 10)
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
