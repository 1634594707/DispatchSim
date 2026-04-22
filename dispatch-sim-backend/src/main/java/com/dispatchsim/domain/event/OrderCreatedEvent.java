package com.dispatchsim.domain.event;

public class OrderCreatedEvent extends AbstractDomainEvent {

    public OrderCreatedEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "OrderCreated";
    }
}
