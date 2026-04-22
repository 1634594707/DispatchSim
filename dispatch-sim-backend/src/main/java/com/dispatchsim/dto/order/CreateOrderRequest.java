package com.dispatchsim.dto.order;

import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建订单请求")
public record CreateOrderRequest(
        @Schema(description = "取货点坐标")
        @NotNull @Valid PositionDto pickup,
        @Schema(description = "送货点坐标")
        @NotNull @Valid PositionDto delivery,
        @Schema(description = "优先级，1 到 10", example = "5")
        @NotNull @Min(1) @Max(10) Integer priority
) {
}
