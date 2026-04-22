package com.dispatchsim.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Position {

    private double x;
    private double y;

    public double distanceTo(Position other) {
        double deltaX = this.x - other.x;
        double deltaY = this.y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    public Position moveTowards(Position target, double maxDistance) {
        double distance = distanceTo(target);
        if (distance == 0 || distance <= maxDistance) {
            return Position.builder().x(target.x).y(target.y).build();
        }

        double ratio = maxDistance / distance;
        return Position.builder()
                .x(this.x + (target.x - this.x) * ratio)
                .y(this.y + (target.y - this.y) * ratio)
                .build();
    }
}
