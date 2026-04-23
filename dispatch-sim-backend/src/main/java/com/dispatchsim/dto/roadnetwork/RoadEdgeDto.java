package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路网边")
public record RoadEdgeDto(
        @Schema(description = "边 ID", example = "1")
        Long id,
        @Schema(description = "起点节点 ID", example = "1")
        Long fromNodeId,
        @Schema(description = "终点节点 ID", example = "2")
        Long toNodeId,
        @Schema(description = "是否双向", example = "true")
        boolean bidirectional,
        @Schema(description = "边权重", example = "80.0")
        double weight,
        @Schema(description = "扩展元数据 JSON", example = "{\"lane\":\"main\"}")
        String metadata
) {
}
