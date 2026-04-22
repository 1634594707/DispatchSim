package com.dispatchsim.dto.simulation;

import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.vehicle.VehicleDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "历史回放帧")
public record ReplayFrameDto(
        @Schema(description = "会话 ID")
        String sessionId,
        @Schema(description = "控制动作")
        ReplayAction action,
        @Schema(description = "游标时间")
        Instant cursorTime,
        @Schema(description = "回放速度", example = "1.0")
        double speed,
        @Schema(description = "进度，0-1", example = "0.5")
        double progress,
        @Schema(description = "已应用事件数", example = "12")
        int appliedEvents,
        @Schema(description = "总事件数", example = "42")
        int totalEvents,
        @Schema(description = "订单快照")
        List<OrderDto> orders,
        @Schema(description = "车辆快照")
        List<VehicleDto> vehicles
) {
}
