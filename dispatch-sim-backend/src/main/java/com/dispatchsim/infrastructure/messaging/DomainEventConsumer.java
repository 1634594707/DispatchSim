package com.dispatchsim.infrastructure.messaging;

import com.dispatchsim.service.DispatchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dispatch-sim.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventConsumer {

    private final DispatchEngine dispatchEngine;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "${dispatch-sim.rabbit.order-queue}")
    public void onOrderEvent(DomainEventMessage event) {
        if (isDuplicate(event.eventId())) {
            return;
        }

        if ("OrderCreated".equals(event.eventType())) {
            dispatchEngine.dispatchPendingOrders();
        }
    }

    @RabbitListener(queues = "${dispatch-sim.rabbit.vehicle-queue}")
    public void onVehicleEvent(DomainEventMessage event) {
        if (isDuplicate(event.eventId())) {
            return;
        }

        if ("VehicleFaulted".equals(event.eventType()) || "VehicleRecovered".equals(event.eventType())) {
            dispatchEngine.dispatchPendingOrders();
        }
    }

    private boolean isDuplicate(String eventId) {
        String key = "dispatchsim:processed:event:" + eventId;
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(6));
        return Boolean.FALSE.equals(absent);
    }
}
