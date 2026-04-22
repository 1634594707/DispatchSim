package com.dispatchsim.infrastructure.messaging;

import java.io.Serializable;

public record DomainEventMessage(
        String eventId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String payload
) implements Serializable {
}
