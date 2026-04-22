package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "批量订单生成结果")
public record BatchOrderResponse(
        @Schema(description = "已生成订单总数", example = "50")
        int totalOrders,
        @Schema(description = "批次数", example = "5")
        int batchesCreated,
        @Schema(description = "使用的生成策略", example = "UNIFORM")
        BatchOrderGenerationStrategy strategy,
        @Schema(description = "请求的批次间隔，单位毫秒", example = "1000")
        long batchIntervalMs,
        @Schema(description = "生成的订单 ID 列表")
        List<Long> orderIds
) {
}
