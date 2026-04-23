package com.dispatchsim.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "current_position_x")),
            @AttributeOverride(name = "y", column = @Column(name = "current_position_y"))
    })
    private Position currentPosition;

    @Column(nullable = false)
    private Integer battery;

    @Column(nullable = false)
    private Double speed;

    @Column(nullable = false)
    private Double maxSpeed;

    @Column(nullable = false)
    private Double currentLoad;

    @Column(nullable = false)
    private Double capacity;

    private Double heading;
    private Integer totalTasks;
    private Double totalDistance;
    private Long currentOrderId;
    @Column(name = "order_queue", nullable = false, length = 1024)
    private String orderQueueJson;
    @Column(nullable = false)
    private Integer loadingTimeRemaining;

    public void assignOrder(Long orderId) {
        ensureState(VehicleStatus.IDLE);
        addToQueue(orderId);
        this.currentOrderId = orderId;
        this.status = VehicleStatus.DELIVERING;
    }

    public void completeOrder() {
        if (this.status != VehicleStatus.DELIVERING) {
            throw new IllegalStateException("Only delivering vehicles can complete orders");
        }
        removeFromQueue(this.currentOrderId);
        List<Long> queue = getOrderQueue();
        this.currentOrderId = queue.isEmpty() ? null : queue.get(0);
        this.status = this.currentOrderId == null ? VehicleStatus.IDLE : VehicleStatus.DELIVERING;
        this.loadingTimeRemaining = 0;
        this.currentLoad = 0.0;
        this.totalTasks = (this.totalTasks == null ? 0 : this.totalTasks) + 1;
    }

    public void markFaulty() {
        if (this.status != VehicleStatus.IDLE && this.status != VehicleStatus.DELIVERING) {
            throw new IllegalStateException("Only idle or delivering vehicles can fail");
        }
        this.status = VehicleStatus.FAULTY;
    }

    public void recoverFromFault() {
        if (this.status != VehicleStatus.FAULTY) {
            throw new IllegalStateException("Only faulty vehicles can recover");
        }
        if (this.currentOrderId == null && !getOrderQueue().isEmpty()) {
            this.currentOrderId = getOrderQueue().get(0);
        }
        this.status = this.currentOrderId == null ? VehicleStatus.IDLE : VehicleStatus.DELIVERING;
    }

    public void release() {
        if (this.currentOrderId != null) {
            removeFromQueue(this.currentOrderId);
        }
        List<Long> queue = getOrderQueue();
        this.currentOrderId = queue.isEmpty() ? null : queue.get(0);
        this.status = this.currentOrderId == null ? VehicleStatus.IDLE : VehicleStatus.DELIVERING;
        this.loadingTimeRemaining = 0;
    }

    public void updatePosition(Position nextPosition) {
        double previousDistance = this.currentPosition == null ? 0.0 : this.currentPosition.distanceTo(nextPosition);
        this.currentPosition = nextPosition;
        this.totalDistance = (this.totalDistance == null ? 0.0 : this.totalDistance) + previousDistance;
    }

    public void consumeBattery(double distance) {
        if (distance <= 0) {
            return;
        }
        int consumption = Math.max(1, (int) Math.ceil(distance / 10.0));
        this.battery = Math.max(0, (this.battery == null ? 0 : this.battery) - consumption);
    }

    public void charge(int amount) {
        if (amount <= 0) {
            return;
        }
        this.battery = Math.min(100, (this.battery == null ? 0 : this.battery) + amount);
    }

    public void startLoading(int seconds) {
        this.loadingTimeRemaining = Math.max(0, seconds);
    }

    public int tickLoading() {
        if (this.loadingTimeRemaining == null || this.loadingTimeRemaining <= 0) {
            this.loadingTimeRemaining = 0;
            return 0;
        }
        this.loadingTimeRemaining = this.loadingTimeRemaining - 1;
        return this.loadingTimeRemaining;
    }

    public List<Long> getOrderQueue() {
        if (this.orderQueueJson == null || this.orderQueueJson.isBlank()) {
            return new ArrayList<>();
        }

        String normalized = this.orderQueueJson.trim();
        if ("[]".equals(normalized)) {
            return new ArrayList<>();
        }

        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            throw new IllegalStateException("Invalid vehicle order queue format");
        }

        String content = normalized.substring(1, normalized.length() - 1).trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();
        for (String token : content.split(",")) {
            result.add(Long.parseLong(token.trim()));
        }
        return result;
    }

    public void setOrderQueue(List<Long> orderIds) {
        List<Long> normalized = orderIds == null ? List.of() : orderIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        this.orderQueueJson = normalized.toString();
    }

    public void addToQueue(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<Long> queue = getOrderQueue();
        if (!queue.contains(orderId)) {
            queue.add(orderId);
            setOrderQueue(queue);
        }
    }

    public void removeFromQueue(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<Long> queue = getOrderQueue();
        queue.removeIf(id -> id.equals(orderId));
        setOrderQueue(queue);
    }

    private void ensureState(VehicleStatus expected) {
        if (!Objects.equals(this.status, expected)) {
            throw new IllegalStateException("Expected vehicle state " + expected + " but was " + this.status);
        }
    }
}
