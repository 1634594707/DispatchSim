package com.dispatchsim.domain.strategy;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class FastestArrivalStrategy implements VehicleSelectionStrategy {

    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.FASTEST_ARRIVAL;
    }

    @Override
    public String description() {
        return "按预计到达取货点时间最短选择车辆";
    }

    @Override
    public Optional<Vehicle> select(Order order, List<Vehicle> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(vehicle -> estimateArrivalSeconds(order, vehicle)));
    }

    @Override
    public double calculateScore(Order order, Vehicle vehicle) {
        return 1.0 / (1.0 + estimateArrivalSeconds(order, vehicle));
    }

    private double estimateArrivalSeconds(Order order, Vehicle vehicle) {
        double distance = vehicle.getCurrentPosition().distanceTo(order.getPickup());
        double speed = vehicle.getSpeed() == null || vehicle.getSpeed() <= 0 ? 1.0 : vehicle.getSpeed();
        return distance / speed;
    }
}
