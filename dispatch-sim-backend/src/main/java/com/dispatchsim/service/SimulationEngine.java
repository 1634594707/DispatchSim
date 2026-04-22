package com.dispatchsim.service;

import com.dispatchsim.domain.model.DispatchStrategy;
import com.dispatchsim.dto.simulation.SimulationStatusDto;

public interface SimulationEngine {

    SimulationStatusDto start();

    SimulationStatusDto stop();

    SimulationStatusDto pause();

    SimulationStatusDto resume();

    SimulationStatusDto tick();

    SimulationStatusDto updateStrategy(DispatchStrategy strategy);

    SimulationStatusDto getStatus();
}
