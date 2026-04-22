package com.dispatchsim.dto.statistics;

import com.dispatchsim.domain.model.DispatchStrategy;

public record StrategyStatisticsItemDto(
        DispatchStrategy strategy,
        long count,
        double percentage
) {
}
