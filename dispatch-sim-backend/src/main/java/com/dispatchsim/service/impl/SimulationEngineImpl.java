package com.dispatchsim.service.impl;

import com.dispatchsim.config.AppProperties;
import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Depot;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.SimulationStatus;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.DepotRepository;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.ReplayEventLogRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.simulation.SimulationStatusDto;
import com.dispatchsim.dto.simulation.UpdateSimulationSpeedRequest;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.PerformanceMetricsService;
import com.dispatchsim.service.SimulationEngine;
import com.dispatchsim.service.SimulationSessionHolder;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationEngineImpl implements SimulationEngine {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DepotRepository depotRepository;
    private final ReplayEventLogRepository replayEventLogRepository;
    private final DispatchEngine dispatchEngine;
    private final AppProperties appProperties;
    private final SimulationSessionHolder simulationSessionHolder;
    private final RealtimeStateCoordinator realtimeStateCoordinator;
    private final PerformanceMetricsService performanceMetricsService;

    private volatile SimulationStatus status = SimulationStatus.STOPPED;
    private final AtomicLong elapsedTime = new AtomicLong(0);
    private volatile double speed = 1.0;
    private volatile boolean stepMode = false;
    private volatile boolean pauseEditingEnabled = false;

    @Override
    public synchronized SimulationStatusDto start() {
        status = SimulationStatus.RUNNING;
        stepMode = false;
        pauseEditingEnabled = false;
        dispatchEngine.dispatchPendingOrders();
        realtimeStateCoordinator.publishStatistics(true);
        log.info("Simulation started");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto stop() {
        status = SimulationStatus.STOPPED;
        elapsedTime.set(0);
        stepMode = false;
        pauseEditingEnabled = false;
        realtimeStateCoordinator.publishStatistics(true);
        simulationSessionHolder.rotateSession();
        log.info("Simulation stopped");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto pause() {
        status = SimulationStatus.PAUSED;
        pauseEditingEnabled = true;
        realtimeStateCoordinator.publishStatistics(false);
        log.info("Simulation paused");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto resume() {
        validateStateConsistency();
        status = SimulationStatus.RUNNING;
        stepMode = false;
        pauseEditingEnabled = false;
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
    @Transactional
    public synchronized SimulationStatusDto executeStep() {
        status = SimulationStatus.PAUSED;
        stepMode = true;
        pauseEditingEnabled = true;
        tickInternal();
        return getStatus();
    }

    @Override
    @Transactional
    public synchronized SimulationStatusDto reset() {
        status = SimulationStatus.STOPPED;
        elapsedTime.set(0);
        stepMode = false;
        pauseEditingEnabled = false;

        orderRepository.deleteAllInBatch();
        resetVehicles();
        replayEventLogRepository.deleteAllInBatch();
        performanceMetricsService.reset();
        simulationSessionHolder.rotateSession();
        realtimeStateCoordinator.publishStatistics(true);
        realtimeStateCoordinator.recordAndPublishOrders(List.of(), true);
        realtimeStateCoordinator.recordAndPublishVehicles(vehicleRepository.findAll(), true);
        log.info("Simulation reset");
        return getStatus();
    }

    @Override
    public synchronized SimulationStatusDto updateSpeed(UpdateSimulationSpeedRequest request) {
        if (request.speed() == null || request.speed() < 0.1 || request.speed() > 10.0) {
            throw new BusinessException("仿真速度必须在 0.1 到 10.0 之间");
        }
        this.speed = request.speed();
        log.info("Simulation speed updated to {}", speed);
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
                elapsedTime.get(),
                speed,
                stepMode,
                pauseEditingEnabled
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
        List<Vehicle> idleVehicles = vehicleRepository.findByStatus(VehicleStatus.IDLE);
        List<Vehicle> chargingVehicles = rechargeIdleVehicles(idleVehicles);

        List<Vehicle> deliveringVehicles = vehicleRepository.findByStatus(VehicleStatus.DELIVERING);
        if (deliveringVehicles.isEmpty() && chargingVehicles.isEmpty()) {
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

            if (vehicle.getLoadingTimeRemaining() != null && vehicle.getLoadingTimeRemaining() > 0) {
                vehicle.tickLoading();
                updatedVehicles.add(vehicle);
                if (vehicle.getLoadingTimeRemaining() == 0) {
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
                continue;
            }

            Position target = order.getStatus() == OrderStatus.ASSIGNED ? order.getPickup() : order.getDelivery();
            Position currentPosition = vehicle.getCurrentPosition() == null ? target : vehicle.getCurrentPosition();
            if (vehicle.getCurrentPosition() == null) {
                vehicle.setCurrentPosition(currentPosition);
            }
            if (vehicle.getBattery() != null && vehicle.getBattery() <= 0) {
                updatedVehicles.add(vehicle);
                continue;
            }
            double movementBudget = Math.max(0.1, vehicle.getSpeed() * speed);
            Position nextPosition = currentPosition.moveTowards(target, movementBudget);
            double distanceTravelled = currentPosition.distanceTo(nextPosition);
            vehicle.updatePosition(nextPosition);
            vehicle.consumeBattery(distanceTravelled);
            vehicle.setHeading(calculateHeading(nextPosition, target));
            updatedVehicles.add(vehicle);

            if (nextPosition.distanceTo(target) <= 1.0) {
                vehicle.startLoading(3);
            }
        }

        updatedVehicles.addAll(chargingVehicles);

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

        if (stepMode) {
            status = SimulationStatus.PAUSED;
        }
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

    private List<Vehicle> rechargeIdleVehicles(List<Vehicle> idleVehicles) {
        if (idleVehicles == null || idleVehicles.isEmpty()) {
            return List.of();
        }

        List<Depot> depots = depotRepository.findAll();
        if (depots.isEmpty()) {
            return List.of();
        }

        List<Vehicle> chargingVehicles = new ArrayList<>();
        for (Vehicle vehicle : idleVehicles) {
            if (vehicle.getCurrentPosition() == null || vehicle.getBattery() == null || vehicle.getBattery() >= 100) {
                continue;
            }
            boolean nearDepot = depots.stream()
                    .anyMatch(depot -> depot.getPosition().distanceTo(vehicle.getCurrentPosition()) <= 10.0);
            if (!nearDepot) {
                continue;
            }
            vehicle.charge(5);
            chargingVehicles.add(vehicle);
        }
        return chargingVehicles;
    }

    private void validateStateConsistency() {
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            if (vehicle.getCurrentOrderId() != null && !vehicle.getOrderQueue().contains(vehicle.getCurrentOrderId())) {
                throw new BusinessException("车辆队列与当前订单不一致: " + vehicle.getId());
            }
            if (vehicle.getLoadingTimeRemaining() != null && vehicle.getLoadingTimeRemaining() < 0) {
                throw new BusinessException("车辆装卸时间非法: " + vehicle.getId());
            }
        }
    }

    private void resetVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .sorted(Comparator.comparing(Vehicle::getId))
                .toList();
        if (vehicles.isEmpty()) {
            return;
        }

        List<Position> spawnPoints = buildResetSpawnPoints();
        for (int index = 0; index < vehicles.size(); index++) {
            Vehicle vehicle = vehicles.get(index);
            Position spawn = spawnPoints.get(index % spawnPoints.size());
            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setCurrentPosition(Position.builder().x(spawn.getX()).y(spawn.getY()).build());
            vehicle.setBattery(100);
            vehicle.setCurrentLoad(0.0);
            vehicle.setHeading(index % 2 == 0 ? 0.0 : 180.0);
            vehicle.setTotalTasks(0);
            vehicle.setTotalDistance(0.0);
            vehicle.setCurrentOrderId(null);
            vehicle.setOrderQueue(List.of());
            vehicle.setLoadingTimeRemaining(0);
        }
        vehicleRepository.saveAll(vehicles);
    }

    private List<Position> buildResetSpawnPoints() {
        List<Position> depots = depotRepository.findAll().stream()
                .map(Depot::getPosition)
                .toList();
        if (!depots.isEmpty()) {
            return depots;
        }
        return List.of(
                Position.builder().x(240).y(560).build(),
                Position.builder().x(360).y(560).build(),
                Position.builder().x(480).y(560).build(),
                Position.builder().x(1040).y(560).build()
        );
    }
}
