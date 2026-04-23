package com.dispatchsim.dto.roadnetwork;

import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路网节点")
public record RoadNodeDto(
        @Schema(description = "节点 ID", example = "1")
        Long id,
        PositionDto position,
        @Schema(description = "节点类型", example = "intersection")
        String type,
        @Schema(description = "扩展元数据 JSON", example = "{\"label\":\"West Gate\"}")
        String metadata
) {
}
