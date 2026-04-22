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

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "pickup_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "pickup_y", nullable = false))
    })
    private Position pickup;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "delivery_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "delivery_y", nullable = false))
    })
    private Position delivery;

    @Column(nullable = false)
    private Integer priority;

    @Column(length = 32)
    private String strategy;

    private Long assignedVehicleId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;

    private String cancellationReason;
    @Column(nullable = false)
    private Integer dispatchAttempts;

    public void markPending() {
        this.status = OrderStatus.PENDING;
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.dispatchAttempts == null) {
            this.dispatchAttempts = 0;
        }
    }

    public void assignToVehicle(Long vehicleId, String strategyName) {
        ensureState(OrderStatus.PENDING);
        this.assignedVehicleId = vehicleId;
        this.strategy = strategyName;
        this.assignedAt = Instant.now();
        this.dispatchAttempts = (this.dispatchAttempts == null ? 0 : this.dispatchAttempts) + 1;
        this.status = OrderStatus.ASSIGNED;
    }

    public void startDelivery() {
        ensureState(OrderStatus.ASSIGNED);
        this.status = OrderStatus.DELIVERING;
        this.startedAt = Instant.now();
    }

    public void complete() {
        if (this.status != OrderStatus.DELIVERING) {
            throw new IllegalStateException("Only delivering orders can be completed");
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Completed or cancelled orders cannot be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
        this.assignedVehicleId = null;
    }

    public void rollbackToPending() {
        if (this.status != OrderStatus.ASSIGNED && this.status != OrderStatus.DELIVERING) {
            throw new IllegalStateException("Only assigned or delivering orders can rollback");
        }
        this.status = OrderStatus.PENDING;
        this.assignedVehicleId = null;
        this.assignedAt = null;
        this.startedAt = null;
        this.strategy = null;
    }

    private void ensureState(OrderStatus expected) {
        if (!Objects.equals(this.status, expected)) {
            throw new IllegalStateException("Expected order state " + expected + " but was " + this.status);
        }
    }
}
