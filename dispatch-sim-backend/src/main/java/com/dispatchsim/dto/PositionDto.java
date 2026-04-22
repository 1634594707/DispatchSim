package com.dispatchsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "二维坐标点")
public record PositionDto(
        @Schema(description = "X 坐标，单位米", example = "12.5")
        @DecimalMin("0.0") @DecimalMax("140.0") double x,
        @Schema(description = "Y 坐标，单位米", example = "18.0")
        @DecimalMin("0.0") @DecimalMax("100.0") double y
) {
}
