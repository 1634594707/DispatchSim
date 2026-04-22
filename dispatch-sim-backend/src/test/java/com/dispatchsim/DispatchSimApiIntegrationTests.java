package com.dispatchsim;

import com.dispatchsim.Support.IntegrationTestDataHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatchSimApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationTestDataHelper dataHelper;

    @BeforeEach
    void setUp() {
        dataHelper.resetState();
    }

    @Test
    void orderVehicleSimulationAndStatisticsApisShouldWorkTogether() throws Exception {
        String createOrderPayload = """
                {
                  "pickup": { "x": 10, "y": 10 },
                  "delivery": { "x": 12, "y": 12 },
                  "priority": 5
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.priority").value(5))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(10));

        mockMvc.perform(get("/api/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STOPPED"))
                .andExpect(jsonPath("$.data.strategy").value("NEAREST_FIRST"));

        mockMvc.perform(post("/api/simulation/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.sessionId").isString());

        mockMvc.perform(post("/api/simulation/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"LOAD_BALANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strategy").value("LOAD_BALANCE"));

        mockMvc.perform(post("/api/simulation/batch-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalOrders": 6,
                                  "batchSize": 2,
                                  "batchIntervalMs": 1000,
                                  "strategy": "UNIFORM",
                                  "pickupRange": { "minX": 5, "maxX": 25, "minY": 5, "maxY": 25 },
                                  "deliveryRange": { "minX": 30, "maxX": 60, "minY": 30, "maxY": 60 },
                                  "priority": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(6))
                .andExpect(jsonPath("$.data.batchesCreated").value(3))
                .andExpect(jsonPath("$.data.strategy").value("UNIFORM"))
                .andExpect(jsonPath("$.data.orderIds.length()").value(6));

        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(7))
                .andExpect(jsonPath("$.data.activeVehicles").isNumber());

        mockMvc.perform(get("/api/statistics/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedOrders").value(7));

        mockMvc.perform(get("/api/statistics/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVehicles").value(10));

        mockMvc.perform(get("/api/statistics/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/statistics/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestDispatchDurationMs").isNumber())
                .andExpect(jsonPath("$.data.averageDispatchDurationMs").isNumber())
                .andExpect(jsonPath("$.data.websocketMessagesLastMinute").isNumber())
                .andExpect(jsonPath("$.data.slowQueryCount").isNumber());

        String sessionId = mockMvc.perform(get("/api/simulation/status"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String extractedSessionId = sessionId.replaceAll(".*\"sessionId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/simulation/replay/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/simulation/replay/sessions/{sessionId}/events", extractedSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/api/simulation/replay/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "action": "SEEK",
                                  "progress": 1.0,
                                  "speed": 1.0,
                                  "step": 1
                                }
                                """.formatted(extractedSessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(extractedSessionId))
                .andExpect(jsonPath("$.data.totalEvents").isNumber())
                .andExpect(jsonPath("$.data.orders").isArray())
                .andExpect(jsonPath("$.data.vehicles").isArray());
    }
}
