package com.dispatchsim.dto.statistics;

public record VehicleStatisticsDto(
        long totalVehicles,
        long idleVehicles,
        long deliveringVehicles,
        long faultyVehicles,
        long offlineVehicles
) {
}
