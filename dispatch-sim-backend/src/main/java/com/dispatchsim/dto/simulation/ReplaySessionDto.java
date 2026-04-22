package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "历史回放会话")
public record ReplaySessionDto(
        @Schema(description = "会话 ID")
        String sessionId,
        @Schema(description = "首个事件时间")
        Instant startedAt,
        @Schema(description = "最后事件时间")
        Instant endedAt,
        @Schema(description = "事件总数", example = "42")
        int eventCount
) {
}
