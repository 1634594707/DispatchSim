package com.dispatchsim.service;

import com.dispatchsim.dto.statistics.PerformanceMetricsDto;

public interface PerformanceMetricsService {

    void recordDispatchDuration(long durationMs, int ordersDispatched);

    void recordWebsocketPublish(String topic, int messageCount);

    void recordSlowQuery(String operation, long elapsedMs);

    PerformanceMetricsDto snapshot();
}
