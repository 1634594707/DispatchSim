package com.dispatchsim.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "性能分析指标")
public record PerformanceMetricsDto(
        @Schema(description = "最近一次调度耗时，单位毫秒", example = "24.0")
        double latestDispatchDurationMs,
        @Schema(description = "平均调度耗时，单位毫秒", example = "18.2")
        double averageDispatchDurationMs,
        @Schema(description = "最近一分钟 WebSocket 推送消息数", example = "42")
        long websocketMessagesLastMinute,
        @Schema(description = "累计慢查询告警数", example = "3")
        long slowQueryCount,
        @Schema(description = "最近慢查询记录")
        List<PerformanceMetricPointDto> slowQueries,
        @Schema(description = "最近调度记录")
        List<PerformanceMetricPointDto> dispatchMetrics,
        @Schema(description = "最近 WebSocket 推送记录")
        List<PerformanceMetricPointDto> websocketMetrics
) {
}
