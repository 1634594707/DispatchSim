package com.dispatchsim.service.impl;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.ReplayEventLog;
import com.dispatchsim.domain.model.ReplayEventType;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.repository.ReplayEventLogRepository;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.simulation.ReplayAction;
import com.dispatchsim.dto.simulation.ReplayEventDto;
import com.dispatchsim.dto.simulation.ReplayFrameDto;
import com.dispatchsim.dto.simulation.ReplaySessionDto;
import com.dispatchsim.dto.support.DomainDtoMapper;
import com.dispatchsim.dto.vehicle.VehicleDto;
import com.dispatchsim.service.ReplayService;
import com.dispatchsim.service.SimulationSessionHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayServiceImpl implements ReplayService {

    private final ReplayEventLogRepository replayEventLogRepository;
    private final SimulationSessionHolder simulationSessionHolder;
    private final DomainDtoMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public void recordOrderStatusChange(Order order) {
        persistEvent(ReplayEventType.ORDER_STATUS_CHANGED, "ORDER", order.getId(), mapper.toDto(order));
    }

    @Override
    public void recordVehicleStatusChange(Vehicle vehicle) {
        persistEvent(ReplayEventType.VEHICLE_STATUS_CHANGED, "VEHICLE", vehicle.getId(), mapper.toDto(vehicle));
    }

    @Override
    public void recordVehiclePositionUpdate(Vehicle vehicle) {
        persistEvent(ReplayEventType.VEHICLE_POSITION_UPDATED, "VEHICLE", vehicle.getId(), mapper.toDto(vehicle));
    }

    @Override
    public List<ReplaySessionDto> listSessions() {
        Map<String, List<ReplayEventLog>> grouped = new LinkedHashMap<>();
        replayEventLogRepository.findAll().stream()
                .sorted(Comparator.comparing(ReplayEventLog::getEventTime))
                .forEach(event -> grouped.computeIfAbsent(event.getSessionId(), key -> new ArrayList<>()).add(event));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ReplayEventLog> events = entry.getValue();
                    return new ReplaySessionDto(
                            entry.getKey(),
                            events.get(0).getEventTime(),
                            events.get(events.size() - 1).getEventTime(),
                            events.size()
                    );
                })
                .sorted(Comparator.comparing(ReplaySessionDto::endedAt).reversed())
                .toList();
    }

    @Override
    public List<ReplayEventDto> findBySessionId(String sessionId) {
        return replayEventLogRepository.findBySessionIdOrderByEventTimeAsc(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<ReplayEventDto> findByTimeRange(Instant startTime, Instant endTime) {
        return replayEventLogRepository.findByEventTimeBetweenOrderByEventTimeAsc(startTime, endTime).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ReplayFrameDto buildFrame(String sessionId, ReplayAction action, Double progress, Double speed, Integer step) {
        List<ReplayEventLog> events = replayEventLogRepository.findBySessionIdOrderByEventTimeAsc(sessionId);
        double resolvedSpeed = speed == null ? 1.0 : speed;
        if (events.isEmpty()) {
            return new ReplayFrameDto(sessionId, action, null, resolvedSpeed, 0.0, 0, 0, List.of(), List.of());
        }

        int totalEvents = events.size();
        int appliedEvents = resolveAppliedEvents(totalEvents, action, progress, step);
        List<ReplayEventLog> slice = events.subList(0, appliedEvents);

        Map<Long, OrderDto> orders = new LinkedHashMap<>();
        Map<Long, VehicleDto> vehicles = new LinkedHashMap<>();
        for (ReplayEventLog event : slice) {
            applyEvent(event, orders, vehicles);
        }

        Instant cursorTime = slice.isEmpty()
                ? events.get(0).getEventTime()
                : slice.get(slice.size() - 1).getEventTime();
        double resolvedProgress = totalEvents == 0 ? 0.0 : (double) appliedEvents / totalEvents;
        return new ReplayFrameDto(
                sessionId,
                action,
                cursorTime,
                resolvedSpeed,
                resolvedProgress,
                appliedEvents,
                totalEvents,
                new ArrayList<>(orders.values()),
                new ArrayList<>(vehicles.values())
        );
    }

    private int resolveAppliedEvents(int totalEvents, ReplayAction action, Double progress, Integer step) {
        if (totalEvents <= 0) {
            return 0;
        }
        if (action == ReplayAction.STEP) {
            return Math.min(totalEvents, Math.max(1, step == null ? 1 : step));
        }
        double safeProgress = progress == null ? 1.0 : Math.max(0.0, Math.min(1.0, progress));
        int computed = (int) Math.ceil(totalEvents * safeProgress);
        return Math.max(1, Math.min(totalEvents, computed));
    }

    private void applyEvent(ReplayEventLog event, Map<Long, OrderDto> orders, Map<Long, VehicleDto> vehicles) {
        try {
            if (event.getEventType() == ReplayEventType.ORDER_STATUS_CHANGED) {
                orders.put(event.getAggregateId(), objectMapper.readValue(event.getPayload(), OrderDto.class));
                return;
            }
            vehicles.put(event.getAggregateId(), objectMapper.readValue(event.getPayload(), VehicleDto.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析回放事件负载: " + event.getId(), exception);
        }
    }

    private void persistEvent(ReplayEventType eventType, String aggregateType, Long aggregateId, Object payload) {
        if (aggregateId == null) {
            return;
        }
        String sessionId = simulationSessionHolder.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            replayEventLogRepository.save(ReplayEventLog.builder()
                    .sessionId(sessionId)
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventTime(Instant.now())
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException exception) {
            log.error("Failed to persist replay event eventType={} aggregateType={} aggregateId={}",
                    eventType, aggregateType, aggregateId, exception);
        }
    }

    private ReplayEventDto toDto(ReplayEventLog event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            return new ReplayEventDto(
                    event.getId(),
                    event.getSessionId(),
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventTime(),
                    payload
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析回放事件负载: " + event.getId(), exception);
        }
    }
}
