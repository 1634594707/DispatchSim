package com.dispatchsim.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "归档订单请求")
public record ArchiveOrderRequest(
        @Schema(description = "归档原因", example = "已完成且需要从活动列表移除")
        String reason
) {
}
