package com.dispatchsim.service;

import com.dispatchsim.domain.event.DomainEvent;

public interface DomainEventPublisher {

    void publish(String aggregateType, Long aggregateId, DomainEvent event);
}
