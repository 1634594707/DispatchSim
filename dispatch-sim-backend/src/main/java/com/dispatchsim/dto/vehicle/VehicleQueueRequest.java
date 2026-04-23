package com.dispatchsim.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "车辆订单队列更新请求")
public record VehicleQueueRequest(
        @Schema(description = "订单 ID 队列", example = "[12,15,18]")
        List<Long> orderIds
) {
}
