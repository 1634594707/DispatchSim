package com.dispatchsim.domain.event;

public class VehicleRecoveredEvent extends AbstractDomainEvent {

    public VehicleRecoveredEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "VehicleRecovered";
    }
}
