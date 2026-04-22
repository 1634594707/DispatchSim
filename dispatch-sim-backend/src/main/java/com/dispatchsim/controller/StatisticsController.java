package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.statistics.OrderStatisticsDto;
import com.dispatchsim.dto.statistics.PerformanceMetricsDto;
import com.dispatchsim.dto.statistics.StatisticsOverviewDto;
import com.dispatchsim.dto.statistics.StrategyStatisticsDto;
import com.dispatchsim.dto.statistics.VehicleStatisticsDto;
import com.dispatchsim.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.STATISTICS)
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "查询订单、车辆与策略统计信息")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "查询总览统计")
    public ApiResponse<StatisticsOverviewDto> overview() {
        return ApiResponse.success(statisticsService.getOverview());
    }

    @GetMapping("/orders")
    @Operation(summary = "查询订单统计")
    public ApiResponse<OrderStatisticsDto> orders() {
        return ApiResponse.success(statisticsService.getOrderStatistics());
    }

    @GetMapping("/vehicles")
    @Operation(summary = "查询车辆统计")
    public ApiResponse<VehicleStatisticsDto> vehicles() {
        return ApiResponse.success(statisticsService.getVehicleStatistics());
    }

    @GetMapping("/strategies")
    @Operation(summary = "查询策略统计")
    public ApiResponse<StrategyStatisticsDto> strategies() {
        return ApiResponse.success(statisticsService.getStrategyStatistics());
    }

    @GetMapping("/performance")
    @Operation(summary = "查询性能分析指标")
    public ApiResponse<PerformanceMetricsDto> performance() {
        return ApiResponse.success(statisticsService.getPerformanceMetrics());
    }
}
