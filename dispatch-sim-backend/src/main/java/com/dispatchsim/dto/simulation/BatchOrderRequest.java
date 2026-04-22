package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "批量订单生成请求")
public record BatchOrderRequest(
        @Schema(description = "总订单数", example = "50")
        @NotNull @Min(1) @Max(1000) Integer totalOrders,
        @Schema(description = "每批生成数量", example = "10")
        @NotNull @Min(1) @Max(200) Integer batchSize,
        @Schema(description = "批次间隔，单位毫秒，仅记录为请求参数", example = "1000")
        @NotNull @Min(0) @Max(60000) Long batchIntervalMs,
        @Schema(description = "批量生成策略", example = "UNIFORM")
        @NotNull BatchOrderGenerationStrategy strategy,
        @Schema(description = "取货点坐标范围")
        @NotNull @Valid BatchOrderRangeDto pickupRange,
        @Schema(description = "送货点坐标范围")
        @NotNull @Valid BatchOrderRangeDto deliveryRange,
        @Schema(description = "订单优先级", example = "5")
        @NotNull @Min(1) @Max(10) Integer priority
) {
}
