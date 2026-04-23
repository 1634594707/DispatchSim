package com.dispatchsim.service;

import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.dto.vehicle.FaultRequest;
import com.dispatchsim.dto.vehicle.VehicleBatteryUpdateRequest;
import com.dispatchsim.dto.vehicle.VehicleChargeRequest;
import com.dispatchsim.dto.vehicle.VehicleDto;
import com.dispatchsim.dto.vehicle.VehicleQueueRequest;
import com.dispatchsim.domain.model.Position;

import java.util.List;

public interface VehicleService {

    List<VehicleDto> listVehicles(VehicleStatus status);

    VehicleDto getVehicle(Long id);

    VehicleDto faultVehicle(Long id, FaultRequest request);

    VehicleDto recoverVehicle(Long id);

    VehicleDto updateVehiclePosition(Long id, Position position);

    VehicleDto updateBattery(Long id, VehicleBatteryUpdateRequest request);

    VehicleDto chargeVehicle(Long id, VehicleChargeRequest request);

    VehicleDto updateOrderQueue(Long id, VehicleQueueRequest request);

    List<Long> getOrderQueue(Long id);
}
