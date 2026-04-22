package com.dispatchsim.domain.event;

public class OrderCompletedEvent extends AbstractDomainEvent {

    public OrderCompletedEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "OrderCompleted";
    }
}
