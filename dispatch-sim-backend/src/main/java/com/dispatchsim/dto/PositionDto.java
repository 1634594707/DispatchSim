package com.dispatchsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "二维坐标点")
public record PositionDto(
        @Schema(description = "X 坐标，单位米", example = "240.0")
        @DecimalMin("0.0") @DecimalMax("1200.0") double x,
        @Schema(description = "Y 坐标，单位米", example = "560.0")
        @DecimalMin("0.0") @DecimalMax("800.0") double y
) {
}
