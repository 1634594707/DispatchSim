package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "批量订单坐标范围")
public record BatchOrderRangeDto(
        @Schema(description = "最小 X 坐标", example = "0")
        @DecimalMin("0.0") @DecimalMax("1200.0") double minX,
        @Schema(description = "最大 X 坐标", example = "400")
        @DecimalMin("0.0") @DecimalMax("1200.0") double maxX,
        @Schema(description = "最小 Y 坐标", example = "0")
        @DecimalMin("0.0") @DecimalMax("800.0") double minY,
        @Schema(description = "最大 Y 坐标", example = "300")
        @DecimalMin("0.0") @DecimalMax("800.0") double maxY
) {
}
