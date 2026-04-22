package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByStatus(VehicleStatus status);

    long countByStatus(VehicleStatus status);
}
