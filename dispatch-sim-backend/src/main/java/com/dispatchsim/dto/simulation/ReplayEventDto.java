package com.dispatchsim.dto.simulation;

import com.dispatchsim.domain.model.ReplayEventType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "历史事件")
public record ReplayEventDto(
        @Schema(description = "事件 ID", example = "1")
        Long id,
        @Schema(description = "会话 ID")
        String sessionId,
        @Schema(description = "事件类型", example = "VEHICLE_POSITION_UPDATED")
        ReplayEventType eventType,
        @Schema(description = "聚合类型", example = "VEHICLE")
        String aggregateType,
        @Schema(description = "聚合 ID", example = "3")
        Long aggregateId,
        @Schema(description = "事件时间")
        Instant eventTime,
        @Schema(description = "事件负载")
        JsonNode payload
) {
}
