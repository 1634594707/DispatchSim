package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新仿真速度请求")
public record UpdateSimulationSpeedRequest(
        @Schema(description = "仿真速度倍率", example = "2.0")
        @NotNull
        @DecimalMin(value = "0.1")
        @DecimalMax(value = "10.0")
        Double speed
) {
}
