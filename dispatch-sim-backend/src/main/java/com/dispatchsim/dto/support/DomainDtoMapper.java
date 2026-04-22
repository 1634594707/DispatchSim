package com.dispatchsim.dto.support;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.vehicle.VehicleDto;
import org.springframework.stereotype.Component;

@Component
public class DomainDtoMapper {

    public OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getStatus(),
                toDto(order.getPickup()),
                toDto(order.getDelivery()),
                order.getPriority(),
                order.getAssignedVehicleId(),
                order.getCreatedAt(),
                order.getCompletedAt(),
                order.getCancellationReason()
        );
    }

    public VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getStatus(),
                toDto(vehicle.getCurrentPosition()),
                vehicle.getBattery(),
                vehicle.getSpeed(),
                vehicle.getMaxSpeed(),
                vehicle.getCurrentLoad(),
                vehicle.getCapacity(),
                vehicle.getHeading(),
                vehicle.getTotalTasks(),
                vehicle.getTotalDistance(),
                vehicle.getCurrentOrderId()
        );
    }

    public PositionDto toDto(Position position) {
        if (position == null) {
            return null;
        }
        return new PositionDto(position.getX(), position.getY());
    }

    public Position toEntity(PositionDto dto) {
        return Position.builder()
                .x(dto.x())
                .y(dto.y())
                .build();
    }
}
