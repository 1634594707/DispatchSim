package com.dispatchsim.dto.vehicle;

import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "车辆信息")
public record VehicleDto(
        @Schema(description = "车辆 ID", example = "1")
        Long id,
        @Schema(description = "车辆状态", example = "IDLE")
        VehicleStatus status,
        @Schema(description = "当前位置")
        PositionDto currentPosition,
        @Schema(description = "电量百分比", example = "85")
        Integer battery,
        @Schema(description = "当前速度", example = "5.0")
        Double speed,
        @Schema(description = "最大速度", example = "8.0")
        Double maxSpeed,
        @Schema(description = "当前载重", example = "0.0")
        Double currentLoad,
        @Schema(description = "最大载重", example = "50.0")
        Double capacity,
        @Schema(description = "朝向角度", example = "90.0")
        Double heading,
        @Schema(description = "累计任务数", example = "12")
        Integer totalTasks,
        @Schema(description = "累计行驶距离", example = "328.5")
        Double totalDistance,
        @Schema(description = "当前订单 ID", example = "18")
        Long currentOrderId
) {
}
