package com.dispatchsim.domain.strategy;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class LoadBalanceStrategy implements VehicleSelectionStrategy {

    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.LOAD_BALANCE;
    }

    @Override
    public String description() {
        return "选择当前累计任务数最少的车辆";
    }

    @Override
    public Optional<Vehicle> select(Order order, List<Vehicle> candidates) {
        return candidates.stream()
                .min(Comparator.comparingInt(vehicle -> vehicle.getTotalTasks() == null ? 0 : vehicle.getTotalTasks()));
    }

    @Override
    public double calculateScore(Order order, Vehicle vehicle) {
        return 1.0 / (1.0 + (vehicle.getTotalTasks() == null ? 0 : vehicle.getTotalTasks()));
    }
}
