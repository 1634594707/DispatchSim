package com.dispatchsim.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "取消订单请求")
public record CancelOrderRequest(@Schema(description = "取消原因", example = "用户主动取消") String reason) {
}
