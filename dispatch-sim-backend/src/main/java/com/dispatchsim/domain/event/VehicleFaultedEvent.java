package com.dispatchsim.domain.event;

public class VehicleFaultedEvent extends AbstractDomainEvent {

    public VehicleFaultedEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "VehicleFaulted";
    }
}
