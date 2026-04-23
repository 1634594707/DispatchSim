package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "完整路网")
public record RoadNetworkDto(
        List<RoadNodeDto> nodes,
        List<RoadEdgeDto> edges,
        @Schema(description = "路网版本号", example = "3")
        int version,
        @Schema(description = "更新时间")
        Instant updatedAt
) {
}
