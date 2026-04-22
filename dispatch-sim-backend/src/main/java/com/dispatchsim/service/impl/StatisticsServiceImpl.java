package com.dispatchsim.service.impl;

import com.dispatchsim.config.AppProperties;
import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.OrderRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.dispatchsim.dto.statistics.OrderStatisticsDto;
import com.dispatchsim.dto.statistics.PerformanceMetricsDto;
import com.dispatchsim.dto.statistics.StatisticsOverviewDto;
import com.dispatchsim.dto.statistics.StrategyStatisticsDto;
import com.dispatchsim.dto.statistics.StrategyStatisticsItemDto;
import com.dispatchsim.dto.statistics.VehicleStatisticsDto;
import com.dispatchsim.service.DispatchEngine;
import com.dispatchsim.service.PerformanceMetricsService;
import com.dispatchsim.service.StatisticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String OVERVIEW_CACHE_KEY = "dispatchsim:statistics:overview";
    private static final String ORDER_CACHE_KEY = "dispatchsim:statistics:orders";
    private static final String VEHICLE_CACHE_KEY = "dispatchsim:statistics:vehicles";
    private static final String STRATEGY_CACHE_KEY = "dispatchsim:statistics:strategies";
    private static final List<String> CACHE_KEYS = List.of(
            OVERVIEW_CACHE_KEY,
            ORDER_CACHE_KEY,
            VEHICLE_CACHE_KEY,
            STRATEGY_CACHE_KEY
    );

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchEngine dispatchEngine;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final PerformanceMetricsService performanceMetricsService;

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    @Override
    public StatisticsOverviewDto getOverview() {
        return getCachedOrCompute(OVERVIEW_CACHE_KEY, StatisticsOverviewDto.class, this::computeOverview);
    }

    @Override
    public OrderStatisticsDto getOrderStatistics() {
        return getCachedOrCompute(ORDER_CACHE_KEY, OrderStatisticsDto.class, this::computeOrderStatistics);
    }

    @Override
    public VehicleStatisticsDto getVehicleStatistics() {
        return getCachedOrCompute(VEHICLE_CACHE_KEY, VehicleStatisticsDto.class, this::computeVehicleStatistics);
    }

    @Override
    public StrategyStatisticsDto getStrategyStatistics() {
        return getCachedOrCompute(STRATEGY_CACHE_KEY, StrategyStatisticsDto.class, this::computeStrategyStatistics);
    }

    @Override
    public PerformanceMetricsDto getPerformanceMetrics() {
        return performanceMetricsService.snapshot();
    }

    @Override
    public StatisticsOverviewDto refreshOverview() {
        StatisticsOverviewDto overview = computeOverview();
        writeCache(OVERVIEW_CACHE_KEY, overview);
        return overview;
    }

    @Override
    public void invalidateCaches() {
        localCache.clear();
        if (!appProperties.getStatistics().isCacheEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(CACHE_KEYS);
        } catch (Exception exception) {
            log.debug("Failed to invalidate statistics redis cache", exception);
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void refreshStatisticsCache() {
        if (!appProperties.getScheduling().isStatisticsCacheRefreshEnabled()) {
            return;
        }
        try {
            writeCaches(Map.of(
                    OVERVIEW_CACHE_KEY, computeOverview(),
                    ORDER_CACHE_KEY, computeOrderStatistics(),
                    VEHICLE_CACHE_KEY, computeVehicleStatistics(),
                    STRATEGY_CACHE_KEY, computeStrategyStatistics()
            ));
        } catch (Exception exception) {
            log.warn("Failed to refresh statistics cache", exception);
        }
    }

    private StatisticsOverviewDto computeOverview() {
        long startedAt = System.nanoTime();
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long deliveringOrders = orderRepository.countByStatus(OrderStatus.DELIVERING);
        long totalVehicles = vehicleRepository.count();
        long deliveringVehicles = vehicleRepository.countByStatus(VehicleStatus.DELIVERING);
        long activeVehicles = vehicleRepository.countByStatus(VehicleStatus.IDLE) + deliveringVehicles;

        List<Order> allOrders = orderRepository.findAll();
        List<Order> completed = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED && order.getCompletedAt() != null)
                .toList();
        List<Order> assigned = allOrders.stream()
                .filter(order -> order.getAssignedAt() != null)
                .toList();
        long reDispatchCount = allOrders.stream()
                .filter(order -> order.getDispatchAttempts() != null && order.getDispatchAttempts() > 1)
                .count();

        double averageDeliveryTime = completed.stream()
                .mapToLong(order -> Duration.between(order.getCreatedAt(), order.getCompletedAt()).toSeconds())
                .average()
                .orElse(0.0);
        double avgWaitingTime = assigned.stream()
                .mapToLong(order -> Duration.between(order.getCreatedAt(), order.getAssignedAt()).toSeconds())
                .average()
                .orElse(0.0);
        double avgDispatchTime = assigned.stream()
                .mapToLong(order -> Duration.between(order.getCreatedAt(), order.getAssignedAt()).toMillis())
                .average()
                .orElse(0.0);
        double completionRate = totalOrders == 0 ? 0.0 : completedOrders * 100.0 / totalOrders;
        double vehicleUtilization = totalVehicles == 0 ? 0.0 : deliveringVehicles * 100.0 / totalVehicles;
        double totalDistance = vehicleRepository.findAll().stream()
                .mapToDouble(vehicle -> vehicle.getTotalDistance() == null ? 0.0 : vehicle.getTotalDistance())
                .sum();
        double averageDistance = totalVehicles == 0 ? 0.0 : totalDistance / totalVehicles;

        StatisticsOverviewDto result = new StatisticsOverviewDto(
                totalOrders,
                completedOrders,
                completionRate,
                averageDeliveryTime,
                vehicleUtilization,
                avgWaitingTime,
                avgDispatchTime,
                reDispatchCount,
                pendingOrders,
                deliveringOrders,
                activeVehicles,
                totalDistance,
                averageDistance
        );
        logIfSlow("statistics.overview", startedAt);
        return result;
    }

    private OrderStatisticsDto computeOrderStatistics() {
        long startedAt = System.nanoTime();
        OrderStatisticsDto result = new OrderStatisticsDto(
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.ASSIGNED),
                orderRepository.countByStatus(OrderStatus.DELIVERING),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED)
        );
        logIfSlow("statistics.orders", startedAt);
        return result;
    }

    private VehicleStatisticsDto computeVehicleStatistics() {
        long startedAt = System.nanoTime();
        VehicleStatisticsDto result = new VehicleStatisticsDto(
                vehicleRepository.count(),
                vehicleRepository.countByStatus(VehicleStatus.IDLE),
                vehicleRepository.countByStatus(VehicleStatus.DELIVERING),
                vehicleRepository.countByStatus(VehicleStatus.FAULTY),
                vehicleRepository.countByStatus(VehicleStatus.OFFLINE)
        );
        logIfSlow("statistics.vehicles", startedAt);
        return result;
    }

    private StrategyStatisticsDto computeStrategyStatistics() {
        long startedAt = System.nanoTime();
        Map<DispatchStrategy, Long> usage = dispatchEngine.getStrategyUsageStats();
        long total = usage.values().stream().mapToLong(Long::longValue).sum();
        List<StrategyStatisticsItemDto> items = Arrays.stream(DispatchStrategy.values())
                .map(strategy -> {
                    long count = usage.getOrDefault(strategy, 0L);
                    double percentage = total == 0 ? 0.0 : count * 100.0 / total;
                    return new StrategyStatisticsItemDto(strategy, count, percentage);
                })
                .toList();
        StrategyStatisticsDto result = new StrategyStatisticsDto(items);
        logIfSlow("statistics.strategies", startedAt);
        return result;
    }

    private <T> T getCachedOrCompute(String key, Class<T> type, Supplier<T> calculator) {
        if (!appProperties.getStatistics().isCacheEnabled()) {
            return calculator.get();
        }

        T localValue = readLocalCache(key, type);
        if (localValue != null) {
            return localValue;
        }

        T redisValue = readRedisCache(key, type);
        if (redisValue != null) {
            storeLocalCache(key, redisValue);
            return redisValue;
        }

        T computed = calculator.get();
        writeCache(key, computed);
        return computed;
    }

    private <T> T readLocalCache(String key, Class<T> type) {
        if (!appProperties.getStatistics().isLocalCacheEnabled()) {
            return null;
        }
        CacheEntry entry = localCache.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            localCache.remove(key);
            return null;
        }
        return type.cast(entry.value());
    }

    private <T> T readRedisCache(String key, Class<T> type) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception exception) {
            log.debug("Statistics cache read failed for key={}", key, exception);
        }
        return null;
    }

    private void writeCache(String key, Object value) {
        storeLocalCache(key, value);
        if (!appProperties.getStatistics().isCacheEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), cacheTtl());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize statistics cache for key " + key, exception);
        } catch (Exception exception) {
            log.debug("Statistics cache write failed for key={}", key, exception);
        }
    }

    private void writeCaches(Map<String, Object> values) {
        values.forEach(this::storeLocalCache);
        if (!appProperties.getStatistics().isCacheEnabled()) {
            return;
        }
        try {
            Duration ttl = cacheTtl();
            Map<String, String> serialized = values.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> serialize(entry.getKey(), entry.getValue())
                    ));
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringConnection = (StringRedisConnection) connection;
                serialized.forEach((key, value) -> stringConnection.setEx(key, ttl.toSeconds(), value));
                return null;
            });
        } catch (Exception exception) {
            log.debug("Statistics cache batch write failed", exception);
        }
    }

    private void storeLocalCache(String key, Object value) {
        if (!appProperties.getStatistics().isLocalCacheEnabled()) {
            return;
        }
        localCache.put(key, new CacheEntry(value, Instant.now().plus(cacheTtl())));
    }

    private String serialize(String key, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize statistics cache for key " + key, exception);
        }
    }

    private Duration cacheTtl() {
        return Duration.ofSeconds(Math.max(1, appProperties.getStatistics().getCacheTtlSeconds()));
    }

    private void logIfSlow(String operation, long startedAt) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (elapsedMs >= 200) {
            performanceMetricsService.recordSlowQuery(operation, elapsedMs);
            log.warn("Slow operation {} took {} ms", operation, elapsedMs);
        }
    }

    private record CacheEntry(Object value, Instant expiresAt) {
    }
}
