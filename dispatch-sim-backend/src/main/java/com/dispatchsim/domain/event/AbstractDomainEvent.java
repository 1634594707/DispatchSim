package com.dispatchsim.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();
    private final Long aggregateId;

    protected AbstractDomainEvent(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    @Override
    @JsonProperty("eventId")
    public UUID eventId() {
        return eventId;
    }

    @Override
    @JsonProperty("occurredAt")
    public Instant occurredAt() {
        return occurredAt;
    }

    @JsonProperty("aggregateId")
    public Long aggregateId() {
        return aggregateId;
    }

    public UUID getEventId() {
        return eventId();
    }

    public Instant getOccurredAt() {
        return occurredAt();
    }

    public Long getAggregateId() {
        return aggregateId();
    }

    public String getEventType() {
        return eventType();
    }
}
