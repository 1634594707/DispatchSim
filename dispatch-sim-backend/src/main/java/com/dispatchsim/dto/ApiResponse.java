package com.dispatchsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "统一 API 响应包装")
@Builder
public record ApiResponse<T>(
        @Schema(description = "业务状态码", example = "200") int code,
        @Schema(description = "响应消息", example = "OK") String message,
        @Schema(description = "响应数据体") T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
