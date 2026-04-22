package com.dispatchsim.infrastructure.websocket;

import com.dispatchsim.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HeartbeatPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final AppProperties appProperties;

    @Scheduled(fixedRate = 10000)
    public void publishHeartbeat() {
        if (!appProperties.getScheduling().isHeartbeatEnabled()) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/events", Map.of(
                "type", "HEARTBEAT",
                "timestamp", Instant.now().toString()
        ));
    }
}
