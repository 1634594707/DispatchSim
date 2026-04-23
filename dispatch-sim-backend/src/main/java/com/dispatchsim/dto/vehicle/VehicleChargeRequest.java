package com.dispatchsim.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "车辆充电请求")
public record VehicleChargeRequest(
        @Schema(description = "充电量", example = "15")
        @Min(1) @Max(100) Integer amount
) {
}
