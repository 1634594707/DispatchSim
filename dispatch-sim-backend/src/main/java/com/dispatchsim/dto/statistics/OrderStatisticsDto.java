package com.dispatchsim.dto.statistics;

public record OrderStatisticsDto(
        long pendingOrders,
        long assignedOrders,
        long deliveringOrders,
        long completedOrders,
        long cancelledOrders
) {
}
