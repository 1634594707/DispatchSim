package com.dispatchsim.dto.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "历史回放控制请求")
public record ReplayControlRequest(
        @Schema(description = "回放会话 ID")
        @NotBlank
        String sessionId,
        @Schema(description = "控制动作", example = "SEEK")
        @NotNull
        ReplayAction action,
        @Schema(description = "进度，0-1", example = "0.5")
        @DecimalMin("0.0")
        Double progress,
        @Schema(description = "回放速度", example = "2.0")
        @DecimalMin("0.1")
        Double speed,
        @Schema(description = "单步步数", example = "1")
        @Min(1)
        Integer step
) {
}
