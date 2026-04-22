package com.dispatchsim.infrastructure.messaging;

import com.dispatchsim.domain.event.DomainEvent;
import com.dispatchsim.domain.repository.OutboxRepository;
import com.dispatchsim.infrastructure.persistence.OutboxEventEntity;
import com.dispatchsim.infrastructure.persistence.OutboxStatus;
import com.dispatchsim.service.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(String aggregateType, Long aggregateId, DomainEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .eventId(event.eventId().toString())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(event.eventType())
                .payload(serialize(event))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
        outboxRepository.save(entity);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize event " + event.eventType(), exception);
        }
    }
}
