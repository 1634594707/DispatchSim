package com.dispatchsim.dto.roadnetwork;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "路网版本快照")
public record RoadNetworkVersionDto(
        @Schema(description = "版本记录 ID", example = "1")
        Long id,
        @Schema(description = "版本号", example = "3")
        int version,
        @Schema(description = "路网快照 JSON")
        String snapshot,
        @Schema(description = "创建时间")
        Instant createdAt
) {
}
