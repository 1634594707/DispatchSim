package com.dispatchsim.dto.statistics;

public record StatisticsOverviewDto(
        long totalOrders,
        long completedOrders,
        double completionRate,
        double averageDeliveryTime,
        double vehicleUtilization,
        double avgWaitingTime,
        double avgDispatchTime,
        long reDispatchCount,
        long pendingOrdersCount,
        long deliveringOrdersCount,
        long activeVehicles,
        double totalDistance,
        double averageDistance
) {
}
