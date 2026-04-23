package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "路径规划结果")
public record RoadNetworkPathDto(
        @Schema(description = "起点节点 ID", example = "1")
        Long startNodeId,
        @Schema(description = "终点节点 ID", example = "15")
        Long endNodeId,
        @Schema(description = "最短路径节点序列")
        List<Long> nodeIds,
        @Schema(description = "路径总权重", example = "520.0")
        double totalWeight
) {
}
