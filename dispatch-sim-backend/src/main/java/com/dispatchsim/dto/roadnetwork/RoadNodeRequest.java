package com.dispatchsim.dto.roadnetwork;

import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "路网节点请求")
public record RoadNodeRequest(
        @NotNull(message = "roadNode.position: 坐标不能为空")
        @Valid
        PositionDto position,
        @NotBlank(message = "roadNode.type: 类型不能为空")
        @Pattern(regexp = "intersection|depot|poi", message = "roadNode.type: 类型必须为 intersection、depot 或 poi")
        String type,
        @Schema(description = "扩展元数据 JSON", example = "{\"label\":\"West Gate\"}")
        String metadata
) {
}
