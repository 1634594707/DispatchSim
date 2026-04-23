package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "路网整体更新请求")
public record BulkRoadNetworkRequest(
        @NotNull(message = "roadNetwork.nodes: 节点列表不能为空")
        @Valid
        List<RoadNodeDto> nodes,
        @NotNull(message = "roadNetwork.edges: 边列表不能为空")
        @Valid
        List<RoadEdgeDto> edges
) {
}
