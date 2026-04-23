package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.RoadNetworkVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoadNetworkVersionRepository extends JpaRepository<RoadNetworkVersion, Long> {

    Optional<RoadNetworkVersion> findTopByOrderByVersionNoDesc();
}
