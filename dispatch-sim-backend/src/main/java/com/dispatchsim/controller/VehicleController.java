package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.vehicle.FaultRequest;
import com.dispatchsim.dto.vehicle.VehicleDto;
import com.dispatchsim.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLES)
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "查询车辆状态，触发故障与恢复")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "查询车辆列表", description = "按状态筛选车辆；不传状态时返回全部车辆")
    public ApiResponse<List<VehicleDto>> listVehicles(@RequestParam(required = false) VehicleStatus status) {
        return ApiResponse.success(vehicleService.listVehicles(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询车辆详情")
    public ApiResponse<VehicleDto> getVehicle(@Parameter(description = "车辆 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(vehicleService.getVehicle(id));
    }

    @PostMapping("/{id}/fault")
    @Operation(summary = "触发车辆故障", description = "将车辆标记为故障，并触发订单重调度")
    public ApiResponse<VehicleDto> faultVehicle(
            @Parameter(description = "车辆 ID", example = "1") @PathVariable Long id,
            @RequestBody(required = false) FaultRequest request) {
        return ApiResponse.success(vehicleService.faultVehicle(id, request == null ? new FaultRequest(null) : request));
    }

    @PostMapping("/{id}/recover")
    @Operation(summary = "恢复车辆", description = "将故障车辆恢复为空闲，或释放当前占用车辆")
    public ApiResponse<VehicleDto> recoverVehicle(@Parameter(description = "车辆 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(vehicleService.recoverVehicle(id));
    }
}
