package com.dispatchsim.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "性能指标点")
public record PerformanceMetricPointDto(
        @Schema(description = "指标名称", example = "dispatch.duration@2026-04-22T21:31:57Z")
        String name,
        @Schema(description = "指标值", example = "24.0")
        double value,
        @Schema(description = "标签信息", example = "orders=3")
        String label
) {
}
