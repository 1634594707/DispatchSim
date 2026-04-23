package com.dispatchsim.dto.depot;

import com.dispatchsim.dto.PositionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "配送中心创建/更新请求")
public record DepotUpsertRequest(
        @Schema(description = "名称", example = "Remote Delivery Hub")
        @NotBlank(message = "depot.name: 名称不能为空")
        @Size(max = 128, message = "depot.name: 名称长度不能超过 128")
        String name,
        @NotNull(message = "depot.position: 坐标不能为空")
        @Valid
        PositionDto position,
        @Schema(description = "图标或短标签", example = "RH")
        @Size(max = 32, message = "depot.icon: 图标长度不能超过 32")
        String icon,
        @Schema(description = "扩展元数据 JSON", example = "{\"zone\":\"remote\"}")
        String metadata
) {
}
