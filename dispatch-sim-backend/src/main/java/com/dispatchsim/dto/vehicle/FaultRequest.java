package com.dispatchsim.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "车辆故障请求")
public record FaultRequest(@Schema(description = "故障类型或说明", example = "battery-low") String faultType) {
}
