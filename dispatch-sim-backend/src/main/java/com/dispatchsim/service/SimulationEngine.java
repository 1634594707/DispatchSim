package com.dispatchsim.service;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.dto.simulation.SimulationStatusDto;
import com.dispatchsim.dto.simulation.UpdateSimulationSpeedRequest;

public interface SimulationEngine {

    SimulationStatusDto start();

    SimulationStatusDto stop();

    SimulationStatusDto pause();

    SimulationStatusDto resume();

    SimulationStatusDto tick();

    SimulationStatusDto executeStep();

    SimulationStatusDto reset();

    SimulationStatusDto updateSpeed(UpdateSimulationSpeedRequest request);

    SimulationStatusDto updateStrategy(DispatchStrategy strategy);

    SimulationStatusDto getStatus();
}
