package com.dispatchsim.domain.strategy;

import com.dispatchsim.domain.model.DispatchStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class VehicleSelectionStrategyRegistry {

    private final Map<DispatchStrategy, VehicleSelectionStrategy> strategies = new EnumMap<>(DispatchStrategy.class);

    public VehicleSelectionStrategyRegistry(List<VehicleSelectionStrategy> strategyList) {
        strategyList.forEach(strategy -> strategies.put(strategy.strategy(), strategy));
    }

    public VehicleSelectionStrategy get(DispatchStrategy dispatchStrategy) {
        VehicleSelectionStrategy strategy = strategies.get(dispatchStrategy);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported strategy: " + dispatchStrategy);
        }
        return strategy;
    }
}
