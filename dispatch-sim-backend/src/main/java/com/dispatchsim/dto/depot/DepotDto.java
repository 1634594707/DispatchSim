package com.dispatchsim.dto.depot;

import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "配送中心")
public record DepotDto(
        @Schema(description = "配送中心 ID", example = "1") Long id,
        @Schema(description = "名称", example = "West Warehouse A") String name,
        PositionDto position,
        @Schema(description = "图标或短标签", example = "WA") String icon,
        @Schema(description = "扩展元数据 JSON", example = "{\"zone\":\"west\"}") String metadata,
        @Schema(description = "创建时间") Instant createdAt
) {
}
