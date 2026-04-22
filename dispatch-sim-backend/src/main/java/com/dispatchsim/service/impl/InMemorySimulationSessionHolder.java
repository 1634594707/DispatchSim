package com.dispatchsim.service.impl;

import com.dispatchsim.service.SimulationSessionHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InMemorySimulationSessionHolder implements SimulationSessionHolder {

    private volatile String sessionId = UUID.randomUUID().toString();

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void rotateSession() {
        sessionId = UUID.randomUUID().toString();
    }
}
