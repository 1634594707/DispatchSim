package com.dispatchsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "分页响应")
public record PageResponse<T>(
        @Schema(description = "当前页，从 0 开始", example = "0")
        int page,
        @Schema(description = "每页条数", example = "20")
        int size,
        @Schema(description = "总记录数", example = "53")
        long totalElements,
        @Schema(description = "总页数", example = "3")
        int totalPages,
        @Schema(description = "当前页数据")
        List<T> items
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent()
        );
    }
}
