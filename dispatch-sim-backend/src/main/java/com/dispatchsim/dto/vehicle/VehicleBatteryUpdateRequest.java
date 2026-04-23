package com.dispatchsim.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新车辆电量请求")
public record VehicleBatteryUpdateRequest(
        @Schema(description = "电量百分比", example = "80")
        @NotNull @Min(0) @Max(100) Integer battery
) {
}
