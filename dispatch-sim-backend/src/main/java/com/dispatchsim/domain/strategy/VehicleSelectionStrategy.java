package com.dispatchsim.domain.strategy;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleSelectionStrategy {

    DispatchStrategy strategy();

    String description();

    Optional<Vehicle> select(Order order, List<Vehicle> candidates);

    double calculateScore(Order order, Vehicle vehicle);
}
