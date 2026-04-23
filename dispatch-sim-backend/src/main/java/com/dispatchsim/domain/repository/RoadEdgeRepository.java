package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.RoadEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadEdgeRepository extends JpaRepository<RoadEdge, Long> {

    List<RoadEdge> findByFromNodeIdOrToNodeId(Long fromNodeId, Long toNodeId);
}
