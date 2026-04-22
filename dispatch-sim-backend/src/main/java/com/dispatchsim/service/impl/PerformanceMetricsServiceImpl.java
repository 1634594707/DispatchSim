package com.dispatchsim.service.impl;

import com.dispatchsim.dto.statistics.PerformanceMetricPointDto;
import com.dispatchsim.dto.statistics.PerformanceMetricsDto;
import com.dispatchsim.service.PerformanceMetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class PerformanceMetricsServiceImpl implements PerformanceMetricsService {

    private static final int MAX_POINTS = 20;

    private final Deque<TimedMetric> dispatchMetrics = new ArrayDeque<>();
    private final Deque<TimedMetric> websocketMetrics = new ArrayDeque<>();
    private final Deque<TimedMetric> slowQueryMetrics = new ArrayDeque<>();

    @Override
    public synchronized void recordDispatchDuration(long durationMs, int ordersDispatched) {
        addMetric(dispatchMetrics, "dispatch.duration", durationMs, "orders=" + ordersDispatched);
    }

    @Override
    public synchronized void recordWebsocketPublish(String topic, int messageCount) {
        addMetric(websocketMetrics, topic, messageCount, "messages=" + messageCount);
    }

    @Override
    public synchronized void recordSlowQuery(String operation, long elapsedMs) {
        addMetric(slowQueryMetrics, operation, elapsedMs, "slow-query");
    }

    @Override
    public synchronized PerformanceMetricsDto snapshot() {
        double latestDispatch = dispatchMetrics.isEmpty() ? 0.0 : dispatchMetrics.getLast().value();
        double averageDispatch = dispatchMetrics.stream().mapToDouble(TimedMetric::value).average().orElse(0.0);
        Instant cutoff = Instant.now().minusSeconds(60);
        long websocketLastMinute = websocketMetrics.stream()
                .filter(metric -> metric.timestamp().isAfter(cutoff))
                .mapToLong(metric -> (long) metric.value())
                .sum();

        return new PerformanceMetricsDto(
                latestDispatch,
                averageDispatch,
                websocketLastMinute,
                slowQueryMetrics.size(),
                toDtoList(slowQueryMetrics),
                toDtoList(dispatchMetrics),
                toDtoList(websocketMetrics)
        );
    }

    private void addMetric(Deque<TimedMetric> target, String name, double value, String label) {
        target.addLast(new TimedMetric(Instant.now(), name, value, label));
        while (target.size() > MAX_POINTS) {
            target.removeFirst();
        }
    }

    private List<PerformanceMetricPointDto> toDtoList(Deque<TimedMetric> source) {
        List<PerformanceMetricPointDto> items = new ArrayList<>(source.size());
        for (TimedMetric metric : source) {
            items.add(new PerformanceMetricPointDto(
                    metric.name() + "@" + metric.timestamp(),
                    metric.value(),
                    metric.label()
            ));
        }
        return items;
    }

    private record TimedMetric(Instant timestamp, String name, double value, String label) {
    }
}
