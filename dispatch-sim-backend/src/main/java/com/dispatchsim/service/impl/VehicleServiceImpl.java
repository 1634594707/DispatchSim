package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.StateException;
import com.dispatchsim.domain.event.VehicleFaultedEvent;
import com.dispatchsim.domain.event.VehicleRecoveredEvent;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.vehicle.FaultRequest;
import com.dispatchsim.dto.vehicle.VehicleBatteryUpdateRequest;
import com.dispatchsim.dto.vehicle.VehicleChargeRequest;
import com.dispatchsim.dto.vehicle.VehicleDto;
import com.dispatchsim.dto.vehicle.VehicleQueueRequest;
import com.dispatchsim.dto.support.DomainDtoMapper;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.DomainEventPublisher;
import com.dispatchsim.service.VehicleService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final OrderRepository orderRepository;
    private final DomainDtoMapper mapper;
    private final DispatchEngine dispatchEngine;
    private final DomainEventPublisher domainEventPublisher;
    private final RealtimeStateCoordinator realtimeStateCoordinator;

    @Override
    public List<VehicleDto> listVehicles(VehicleStatus status) {
        List<Vehicle> vehicles = status == null ? vehicleRepository.findAll() : vehicleRepository.findByStatus(status);
        return vehicles.stream().map(mapper::toDto).toList();
    }

    @Override
    public VehicleDto getVehicle(Long id) {
        return mapper.toDto(findVehicle(id));
    }

    @Override
    @Transactional
    public VehicleDto faultVehicle(Long id, FaultRequest request) {
        log.info("Faulting vehicle {} with faultType={}", id, request == null ? null : request.faultType());
        Vehicle vehicle = findVehicle(id);
        try {
            vehicle.markFaulty();
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }

        orderRepository.findByAssignedVehicleIdAndStatusIn(
                id,
                List.of(OrderStatus.ASSIGNED, OrderStatus.DELIVERING)
        ).ifPresent(order -> {
            order.rollbackToPending();
            orderRepository.save(order);
            realtimeStateCoordinator.recordAndPublishOrders(List.of(order), false);
            vehicle.setCurrentOrderId(null);
        });

        Vehicle saved = vehicleRepository.save(vehicle);
        domainEventPublisher.publish("VEHICLE", saved.getId(), new VehicleFaultedEvent(saved.getId()));
        dispatchEngine.dispatchPendingOrders();
        realtimeStateCoordinator.recordAndPublishVehicles(List.of(saved), true);
        log.info("Vehicle {} faulted", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public VehicleDto updateBattery(Long id, VehicleBatteryUpdateRequest request) {
        Vehicle vehicle = findVehicle(id);
        vehicle.setBattery(request.battery());
        Vehicle saved = vehicleRepository.save(vehicle);
        realtimeStateCoordinator.recordAndPublishVehicles(List.of(saved), true);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public VehicleDto chargeVehicle(Long id, VehicleChargeRequest request) {
        Vehicle vehicle = findVehicle(id);
        vehicle.charge(request == null || request.amount() == null ? 10 : request.amount());
        Vehicle saved = vehicleRepository.save(vehicle);
        realtimeStateCoordinator.recordAndPublishVehicles(List.of(saved), true);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public VehicleDto updateOrderQueue(Long id, VehicleQueueRequest request) {
        Vehicle vehicle = findVehicle(id);
        List<Long> orderIds = request == null || request.orderIds() == null ? List.of() : request.orderIds();
        vehicle.setOrderQueue(orderIds);
        if (vehicle.getCurrentOrderId() == null || !vehicle.getOrderQueue().contains(vehicle.getCurrentOrderId())) {
            vehicle.setCurrentOrderId(vehicle.getOrderQueue().isEmpty() ? null : vehicle.getOrderQueue().get(0));
        }
        if (vehicle.getStatus() != VehicleStatus.FAULTY && vehicle.getStatus() != VehicleStatus.OFFLINE) {
            vehicle.setStatus(vehicle.getCurrentOrderId() == null ? VehicleStatus.IDLE : VehicleStatus.DELIVERING);
        }
        Vehicle saved = vehicleRepository.save(vehicle);
        realtimeStateCoordinator.recordAndPublishVehicles(List.of(saved), true);
        return mapper.toDto(saved);
    }

    @Override
    public List<Long> getOrderQueue(Long id) {
        return findVehicle(id).getOrderQueue();
    }

    @Override
    @Transactional
    public VehicleDto recoverVehicle(Long id) {
        log.info("Recovering vehicle {}", id);
        Vehicle vehicle = findVehicle(id);
        try {
            if (vehicle.getStatus() == VehicleStatus.FAULTY) {
                vehicle.recoverFromFault();
            } else {
                vehicle.release();
            }
        } catch (IllegalStateException exception) {
            throw new StateException(exception.getMessage());
        }
        Vehicle saved = vehicleRepository.save(vehicle);
        domainEventPublisher.publish("VEHICLE", saved.getId(), new VehicleRecoveredEvent(saved.getId()));
        dispatchEngine.dispatchPendingOrders();
        realtimeStateCoordinator.recordAndPublishVehicles(List.of(saved), true);
        log.info("Vehicle {} recovered/released with status {}", saved.getId(), saved.getStatus());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public VehicleDto updateVehiclePosition(Long id, Position position) {
        Vehicle vehicle = findVehicle(id);
        vehicle.updatePosition(position);
        Vehicle saved = vehicleRepository.save(vehicle);
        realtimeStateCoordinator.recordVehiclePositions(List.of(saved));
        realtimeStateCoordinator.publishVehiclePositions(List.of(saved), List.of());
        return mapper.toDto(saved);
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("车辆不存在: " + id));
    }
}
