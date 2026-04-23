package com.dispatchsim.service;

import com.dispatchsim.dto.roadnetwork.RoadNetworkPathDto;

public interface PathPlanningService {

    RoadNetworkPathDto findPath(Long startNodeId, Long endNodeId);
}
