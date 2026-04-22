package com.dispatchsim.infrastructure.messaging;

import com.dispatchsim.config.AppProperties;
import com.dispatchsim.domain.repository.OutboxRepository;
import com.dispatchsim.infrastructure.persistence.OutboxEventEntity;
import com.dispatchsim.infrastructure.persistence.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dispatch-sim.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {

    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEventEntity event : pendingEvents) {
            try {
                String routingKey = "VEHICLE".equalsIgnoreCase(event.getAggregateType()) ? "vehicle" : "order";
                rabbitTemplate.convertAndSend(appProperties.getRabbit().getEventExchange(), routingKey, new DomainEventMessage(
                        event.getEventId(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getEventType(),
                        event.getPayload()
                ));
                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception exception) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                    event.setStatus(OutboxStatus.FAILED);
                    rabbitTemplate.convertAndSend(appProperties.getRabbit().getDeadLetterExchange(), "dead-letter", new DomainEventMessage(
                            event.getEventId(),
                            event.getAggregateType(),
                            event.getAggregateId(),
                            event.getEventType(),
                            event.getPayload()
                    ));
                }
                outboxRepository.save(event);
            }
        }
    }
}
