package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "路网边请求")
public record RoadEdgeRequest(
        @NotNull(message = "roadEdge.fromNodeId: 起点节点不能为空")
        Long fromNodeId,
        @NotNull(message = "roadEdge.toNodeId: 终点节点不能为空")
        Long toNodeId,
        @Schema(description = "是否双向", example = "true")
        boolean bidirectional,
        @DecimalMin(value = "0.0", inclusive = false, message = "roadEdge.weight: 权重必须大于 0")
        Double weight,
        @Schema(description = "扩展元数据 JSON", example = "{\"lane\":\"main\"}")
        String metadata
) {
}
