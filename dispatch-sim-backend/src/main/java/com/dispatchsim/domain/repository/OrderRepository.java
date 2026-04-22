package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    long countByStatus(OrderStatus status);

    List<Order> findByStrategyIsNotNull();

    Optional<Order> findByAssignedVehicleIdAndStatusIn(Long vehicleId, List<OrderStatus> statuses);
}
