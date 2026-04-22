package com.dispatchsim.service;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.dto.simulation.ReplayAction;
import com.dispatchsim.dto.simulation.ReplayEventDto;
import com.dispatchsim.dto.simulation.ReplayFrameDto;
import com.dispatchsim.dto.simulation.ReplaySessionDto;

import java.time.Instant;
import java.util.List;

public interface ReplayService {

    void recordOrderStatusChange(Order order);

    void recordVehicleStatusChange(Vehicle vehicle);

    void recordVehiclePositionUpdate(Vehicle vehicle);

    List<ReplaySessionDto> listSessions();

    List<ReplayEventDto> findBySessionId(String sessionId);

    List<ReplayEventDto> findByTimeRange(Instant startTime, Instant endTime);

    ReplayFrameDto buildFrame(String sessionId, ReplayAction action, Double progress, Double speed, Integer step);
}
