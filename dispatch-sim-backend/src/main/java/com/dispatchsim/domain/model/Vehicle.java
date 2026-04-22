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

    public void assignOrder(Long orderId) {
        ensureState(VehicleStatus.IDLE);
        this.currentOrderId = orderId;
        this.status = VehicleStatus.DELIVERING;
    }

    public void completeOrder() {
        if (this.status != VehicleStatus.DELIVERING) {
            throw new IllegalStateException("Only delivering vehicles can complete orders");
        }
        this.currentOrderId = null;
        this.status = VehicleStatus.IDLE;
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
        this.currentOrderId = null;
        this.status = VehicleStatus.IDLE;
    }

    public void release() {
        this.currentOrderId = null;
        this.status = VehicleStatus.IDLE;
    }

    public void updatePosition(Position nextPosition) {
        double previousDistance = this.currentPosition == null ? 0.0 : this.currentPosition.distanceTo(nextPosition);
        this.currentPosition = nextPosition;
        this.totalDistance = (this.totalDistance == null ? 0.0 : this.totalDistance) + previousDistance;
    }

    private void ensureState(VehicleStatus expected) {
        if (!Objects.equals(this.status, expected)) {
            throw new IllegalStateException("Expected vehicle state " + expected + " but was " + this.status);
        }
    }
}
