package com.dispatchsim.dto.order;

import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "订单信息")
public record OrderDto(
        @Schema(description = "订单 ID", example = "1")
        Long id,
        @Schema(description = "订单状态", example = "ASSIGNED")
        OrderStatus status,
        @Schema(description = "取货点")
        PositionDto pickup,
        @Schema(description = "送货点")
        PositionDto delivery,
        @Schema(description = "优先级", example = "5")
        Integer priority,
        @Schema(description = "分配车辆 ID", example = "3")
        Long assignedVehicleId,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "完成时间")
        Instant completedAt,
        @Schema(description = "取消原因")
        String cancellationReason,
        @Schema(description = "是否已归档")
        Boolean archived,
        @Schema(description = "归档时间")
        Instant archivedAt,
        @Schema(description = "归档原因")
        String archivalReason,
        @Schema(description = "所属出货点 ID", example = "2")
        Long depotId
) {
}
