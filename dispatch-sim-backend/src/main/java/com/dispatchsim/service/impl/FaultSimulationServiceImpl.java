package com.dispatchsim.service.impl;

import com.dispatchsim.service.FaultSimulationService;
import org.springframework.stereotype.Service;

@Service
public class FaultSimulationServiceImpl implements FaultSimulationService {

    @Override
    public void triggerRandomFaults() {
        // Reserved for future random fault simulation.
    }
}
