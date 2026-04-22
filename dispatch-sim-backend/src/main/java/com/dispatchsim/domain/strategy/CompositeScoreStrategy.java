package com.dispatchsim.domain.strategy;

import com.dispatchsim.common.utils.MathUtils;
import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class CompositeScoreStrategy implements VehicleSelectionStrategy {

    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.COMPOSITE_SCORE;
    }

    @Override
    public String description() {
        return "按距离、负载、电量综合评分";
    }

    @Override
    public Optional<Vehicle> select(Order order, List<Vehicle> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(vehicle -> calculateScore(order, vehicle)));
    }

    @Override
    public double calculateScore(Order order, Vehicle vehicle) {
        double distance = vehicle.getCurrentPosition().distanceTo(order.getPickup());
        double distanceScore = 1.0 - MathUtils.clamp(distance / 200.0, 0.0, 1.0);
        double loadScore = 1.0 - MathUtils.clamp((vehicle.getTotalTasks() == null ? 0 : vehicle.getTotalTasks()) / 10.0, 0.0, 1.0);
        double batteryScore = MathUtils.clamp(vehicle.getBattery() / 100.0, 0.0, 1.0);
        return distanceScore * 0.6 + loadScore * 0.3 + batteryScore * 0.1;
    }
}
