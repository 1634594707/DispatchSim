package com.dispatchsim.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "road_edges")
public class RoadEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromNodeId;

    @Column(nullable = false)
    private Long toNodeId;

    @Column(nullable = false)
    private boolean bidirectional;

    @Column(nullable = false)
    private double weight;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
