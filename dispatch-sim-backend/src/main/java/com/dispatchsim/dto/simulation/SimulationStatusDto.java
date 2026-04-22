package com.dispatchsim.dto.simulation;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仿真状态信息")
public record SimulationStatusDto(
        @Schema(description = "仿真运行状态", example = "RUNNING")
        SimulationStatus status,
        @Schema(description = "当前调度策略", example = "NEAREST_FIRST")
        DispatchStrategy strategy,
        @Schema(description = "当前回放会话 ID")
        String sessionId,
        @Schema(description = "已运行时长，单位秒", example = "120")
        long elapsedTime
) {
}
