package com.dispatchsim.dto.simulation;

import com.dispatchsim.domain.model.DispatchStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "切换调度策略请求")
public record UpdateStrategyRequest(
        @Schema(description = "目标调度策略", example = "LOAD_BALANCE")
        @NotNull DispatchStrategy strategy) {
}
