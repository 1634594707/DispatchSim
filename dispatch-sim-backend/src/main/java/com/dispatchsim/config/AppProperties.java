package com.dispatchsim.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "dispatch-sim")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Websocket websocket = new Websocket();
    private final Rabbit rabbit = new Rabbit();
    private final Messaging messaging = new Messaging();
    private final Scheduling scheduling = new Scheduling();
    private final Statistics statistics = new Statistics();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        private boolean allowCredentials = true;
    }

    @Getter
    @Setter
    public static class Websocket {
        private String endpoint = "/ws/simulation";
        private String appDestinationPrefix = "/app";
        private String topicPrefix = "/topic";
    }

    @Getter
    @Setter
    public static class Rabbit {
        private String eventExchange = "dispatchsim.events";
        private String deadLetterExchange = "dispatchsim.events.dlx";
        private String orderQueue = "dispatchsim.order.events";
        private String vehicleQueue = "dispatchsim.vehicle.events";
        private String deadLetterQueue = "dispatchsim.dead-letter";
    }

    @Getter
    @Setter
    public static class Messaging {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Scheduling {
        private boolean simulationTickEnabled = true;
        private boolean heartbeatEnabled = true;
        private boolean statisticsCacheRefreshEnabled = true;
    }

    @Getter
    @Setter
    public static class Statistics {
        private boolean cacheEnabled = true;
        private boolean localCacheEnabled = true;
        private long cacheTtlSeconds = 15;
    }
}
