package com.dispatchsim.domain.event;

public class VehicleAssignedEvent extends AbstractDomainEvent {

    public VehicleAssignedEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "VehicleAssigned";
    }
}
