package com.dispatchsim.service.impl;

import com.dispatchsim.config.AppProperties;
import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.SimulationStatus;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.simulation.SimulationStatusDto;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.SimulationEngine;
import com.dispatchsim.service.SimulationSessionHolder;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationEngineImpl implements SimulationEngine {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchEngine dispatchEngine;
    private final AppProperties appProperties;
    private final SimulationSessionHolder simulationSessionHolder;
    private final RealtimeStateCoordinator realtimeStateCoordinator;

    private volatile SimulationStatus status = SimulationStatus.STOPPED;
    private final AtomicLong elapsedTime = new AtomicLong(0);

    @Override
    public synchronized SimulationStatusDto start() {
        status = SimulationStatus.RUNNING;
        dispatchEngine.dispatchPendingOrders();
        realtimeStateCoordinator.publishStatistics(true);
        log.info("Simulation started");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto stop() {
        status = SimulationStatus.STOPPED;
        elapsedTime.set(0);
        realtimeStateCoordinator.publishStatistics(true);
        simulationSessionHolder.rotateSession();
        log.info("Simulation stopped");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto pause() {
        status = SimulationStatus.PAUSED;
        realtimeStateCoordinator.publishStatistics(false);
        log.info("Simulation paused");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto resume() {
        status = SimulationStatus.RUNNING;
        realtimeStateCoordinator.publishStatistics(false);
        log.info("Simulation resumed");
        return getStatus();
    }

    @Override
    @Transactional
    public synchronized SimulationStatusDto tick() {
        tickInternal();
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto updateStrategy(DispatchStrategy strategy) {
        dispatchEngine.setCurrentStrategy(strategy);
        log.info("Simulation strategy updated to {}", strategy);
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto getStatus() {
        return new SimulationStatusDto(
                status,
                dispatchEngine.getCurrentStrategy(),
                simulationSessionHolder.getSessionId(),
                elapsedTime.get()
        );
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public synchronized void scheduledTick() {
        if (!appProperties.getScheduling().isSimulationTickEnabled()) {
            return;
        }
        if (status != SimulationStatus.RUNNING) {
            return;
        }
        tickInternal();
    }

    private void tickInternal() {
        if (status == SimulationStatus.STOPPED) {
            return;
        }

        long startedAt = System.nanoTime();
        elapsedTime.incrementAndGet();
        dispatchEngine.dispatchPendingOrders();

        List<Vehicle> deliveringVehicles = vehicleRepository.findByStatus(VehicleStatus.DELIVERING);
        if (deliveringVehicles.isEmpty()) {
            realtimeStateCoordinator.publishStatistics(false);
            return;
        }

        List<Vehicle> updatedVehicles = new ArrayList<>();
        List<Order> updatedOrders = new ArrayList<>();
        List<Vehicle> statusChangedVehicles = new ArrayList<>();

        for (Vehicle vehicle : deliveringVehicles) {
            if (vehicle.getCurrentOrderId() == null) {
                continue;
            }

            Order order = orderRepository.findById(vehicle.getCurrentOrderId()).orElse(null);
            if (order == null || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
                vehicle.release();
                updatedVehicles.add(vehicle);
                statusChangedVehicles.add(vehicle);
                continue;
            }

            Position target = order.getStatus() == OrderStatus.ASSIGNED ? order.getPickup() : order.getDelivery();
            Position nextPosition = vehicle.getCurrentPosition().moveTowards(target, vehicle.getSpeed());
            vehicle.updatePosition(nextPosition);
            vehicle.setHeading(calculateHeading(nextPosition, target));
            updatedVehicles.add(vehicle);

            if (nextPosition.distanceTo(target) <= 1.0) {
                if (order.getStatus() == OrderStatus.ASSIGNED) {
                    order.startDelivery();
                    updatedOrders.add(order);
                } else if (order.getStatus() == OrderStatus.DELIVERING) {
                    order.complete();
                    vehicle.completeOrder();
                    updatedOrders.add(order);
                    statusChangedVehicles.add(vehicle);
                }
            }
        }

        if (!updatedVehicles.isEmpty()) {
            vehicleRepository.saveAll(updatedVehicles);
            realtimeStateCoordinator.recordVehiclePositions(updatedVehicles);
            realtimeStateCoordinator.recordVehicleStatuses(statusChangedVehicles);
        }
        if (!updatedOrders.isEmpty()) {
            orderRepository.saveAll(updatedOrders);
            realtimeStateCoordinator.recordOrders(updatedOrders);
        }

        realtimeStateCoordinator.publishVehiclePositions(updatedVehicles, updatedOrders);
        if (!updatedOrders.isEmpty()) {
            realtimeStateCoordinator.publishOrders(updatedOrders);
        }
        realtimeStateCoordinator.publishStatistics(!updatedVehicles.isEmpty() || !updatedOrders.isEmpty());
        logSlowTick(startedAt, updatedVehicles.size(), updatedOrders.size());
    }

    private double calculateHeading(Position current, Position target) {
        return Math.toDegrees(Math.atan2(target.getY() - current.getY(), target.getX() - current.getX()));
    }

    private void logSlowTick(long startedAt, int updatedVehicles, int updatedOrders) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (elapsedMs >= 200) {
            log.warn("Simulation tick took {} ms, updatedVehicles={}, updatedOrders={}", elapsedMs, updatedVehicles, updatedOrders);
        }
    }
}
