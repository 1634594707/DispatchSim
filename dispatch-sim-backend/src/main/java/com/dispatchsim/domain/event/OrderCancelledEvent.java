package com.dispatchsim.domain.event;

public class OrderCancelledEvent extends AbstractDomainEvent {

    public OrderCancelledEvent(Long aggregateId) {
        super(aggregateId);
    }

    @Override
    public String eventType() {
        return "OrderCancelled";
    }
}
