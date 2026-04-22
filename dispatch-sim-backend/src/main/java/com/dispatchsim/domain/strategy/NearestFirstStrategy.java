package com.dispatchsim.domain.strategy;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class NearestFirstStrategy implements VehicleSelectionStrategy {

    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.NEAREST_FIRST;
    }

    @Override
    public String description() {
        return "选择距离取货点最近的空闲车辆";
    }

    @Override
    public Optional<Vehicle> select(Order order, List<Vehicle> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(vehicle -> vehicle.getCurrentPosition().distanceTo(order.getPickup())));
    }

    @Override
    public double calculateScore(Order order, Vehicle vehicle) {
        return 1.0 / (1.0 + vehicle.getCurrentPosition().distanceTo(order.getPickup()));
    }
}
