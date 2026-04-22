package com.dispatchsim.service;

import com.dispatchsim.dto.statistics.OrderStatisticsDto;
import com.dispatchsim.dto.statistics.PerformanceMetricsDto;
import com.dispatchsim.dto.statistics.StatisticsOverviewDto;
import com.dispatchsim.dto.statistics.StrategyStatisticsDto;
import com.dispatchsim.dto.statistics.VehicleStatisticsDto;

public interface StatisticsService {

    StatisticsOverviewDto getOverview();

    OrderStatisticsDto getOrderStatistics();

    VehicleStatisticsDto getVehicleStatistics();

    StrategyStatisticsDto getStrategyStatistics();

    PerformanceMetricsDto getPerformanceMetrics();

    StatisticsOverviewDto refreshOverview();

    void invalidateCaches();
}
