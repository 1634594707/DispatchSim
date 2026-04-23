package com.dispatchsim.infrastructure.persistence;

import com.dispatchsim.domain.model.RoadEdge;
import com.dispatchsim.domain.model.RoadNetworkVersion;
import com.dispatchsim.domain.model.RoadNode;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.Depot;
import com.dispatchsim.domain.model.Vehicle;
import com.dispatchsim.domain.model.VehicleStatus;
import com.dispatchsim.domain.repository.DepotRepository;
import com.dispatchsim.domain.repository.RoadEdgeRepository;
import com.dispatchsim.domain.repository.RoadNetworkVersionRepository;
import com.dispatchsim.domain.repository.RoadNodeRepository;
import com.dispatchsim.domain.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final List<Position> DEFAULT_SPAWN_POINTS = List.of(
            point(240, 560),
            point(360, 560),
            point(480, 560),
            point(240, 400),
            point(360, 400),
            point(480, 400),
            point(620, 400),
            point(760, 400),
            point(900, 400),
            point(1040, 560),
            point(1040, 400),
            point(1040, 240)
    );

    private static final List<RoadNode> DEFAULT_ROAD_NODES = List.of(
            roadNode(60, 400, "poi", "{\"label\":\"West Gate\"}"),
            roadNode(140, 400, "intersection", "{\"label\":\"Gate Plaza\"}"),
            roadNode(240, 400, "intersection", "{\"label\":\"West Yard West\"}"),
            roadNode(360, 400, "intersection", "{\"label\":\"West Yard East\"}"),
            roadNode(480, 400, "intersection", "{\"label\":\"Campus East Junction\"}"),
            roadNode(240, 560, "depot", "{\"label\":\"Warehouse A\"}"),
            roadNode(360, 560, "depot", "{\"label\":\"Warehouse B\"}"),
            roadNode(240, 240, "poi", "{\"label\":\"Parking Court\"}"),
            roadNode(360, 240, "poi", "{\"label\":\"Admin / Workshop\"}"),
            roadNode(480, 560, "depot", "{\"label\":\"Cross Dock\"}"),
            roadNode(620, 400, "intersection", "{\"label\":\"Connector West\"}"),
            roadNode(760, 400, "intersection", "{\"label\":\"Connector Mid\"}"),
            roadNode(900, 400, "intersection", "{\"label\":\"Connector East\"}"),
            roadNode(1040, 400, "intersection", "{\"label\":\"Remote Hub Core\"}"),
            roadNode(1040, 560, "depot", "{\"label\":\"Remote Delivery Hub\"}"),
            roadNode(1040, 240, "poi", "{\"label\":\"Retail Cluster\"}"),
            roadNode(900, 560, "poi", "{\"label\":\"East Logistics Park\"}"),
            roadNode(900, 240, "poi", "{\"label\":\"Residential District\"}"),
            roadNode(1120, 560, "poi", "{\"label\":\"Terminal Court\"}"),
            roadNode(1120, 240, "poi", "{\"label\":\"Commercial Strip\"}")
    );

    private static final int[][] DEFAULT_EDGE_PAIRS = {
            {1, 2}, {2, 3}, {3, 4}, {4, 5}, {3, 6}, {4, 7}, {3, 8}, {4, 9}, {5, 10},
            {6, 7}, {8, 9}, {6, 8}, {7, 9}, {5, 11}, {11, 12}, {12, 13}, {13, 14}, {13, 17},
            {13, 18}, {14, 15}, {14, 16}, {15, 19}, {16, 20}, {17, 15}, {18, 16}, {17, 18}, {19, 20}
    };

    private static final List<Depot> DEFAULT_DEPOTS = List.of(
            depot("西区仓一", 240, 560, "warehouse", "{\"zone\":\"west-yard\",\"roadNodeLabel\":\"Warehouse A\"}"),
            depot("西区仓二", 360, 560, "warehouse", "{\"zone\":\"west-yard\",\"roadNodeLabel\":\"Warehouse B\"}"),
            depot("交叉转运站", 480, 560, "cross-dock", "{\"zone\":\"east-junction\",\"roadNodeLabel\":\"Cross Dock\"}"),
            depot("远端配送中心", 1040, 560, "hub", "{\"zone\":\"remote-hub\",\"roadNodeLabel\":\"Remote Delivery Hub\"}")
    );

    private final DepotRepository depotRepository;
    private final VehicleRepository vehicleRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final RoadNetworkVersionRepository roadNetworkVersionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        initializeRoadNetwork();
        initializeDepots();

        List<Vehicle> existingVehicles = vehicleRepository.findAll();
        if (existingVehicles.isEmpty()) {
            vehicleRepository.saveAll(createDefaultFleet());
            return;
        }

        if (shouldRelocateLegacyFleet(existingVehicles)) {
            relocateLegacyFleet(existingVehicles);
            vehicleRepository.saveAll(existingVehicles);
        }
    }

    @SneakyThrows
    private void initializeRoadNetwork() {
        if (!roadNodeRepository.findAll().isEmpty()) {
            return;
        }

        List<RoadNode> savedNodes = roadNodeRepository.saveAll(DEFAULT_ROAD_NODES.stream()
                .map(template -> RoadNode.builder()
                        .position(point(template.getPosition().getX(), template.getPosition().getY()))
                        .type(template.getType())
                        .metadata(template.getMetadata())
                        .build())
                .toList());
        Map<Integer, RoadNode> nodeMap = Map.ofEntries(
                Map.entry(1, savedNodes.get(0)),
                Map.entry(2, savedNodes.get(1)),
                Map.entry(3, savedNodes.get(2)),
                Map.entry(4, savedNodes.get(3)),
                Map.entry(5, savedNodes.get(4)),
                Map.entry(6, savedNodes.get(5)),
                Map.entry(7, savedNodes.get(6)),
                Map.entry(8, savedNodes.get(7)),
                Map.entry(9, savedNodes.get(8)),
                Map.entry(10, savedNodes.get(9)),
                Map.entry(11, savedNodes.get(10)),
                Map.entry(12, savedNodes.get(11)),
                Map.entry(13, savedNodes.get(12)),
                Map.entry(14, savedNodes.get(13)),
                Map.entry(15, savedNodes.get(14)),
                Map.entry(16, savedNodes.get(15)),
                Map.entry(17, savedNodes.get(16)),
                Map.entry(18, savedNodes.get(17)),
                Map.entry(19, savedNodes.get(18)),
                Map.entry(20, savedNodes.get(19))
        );

        List<RoadEdge> edges = java.util.Arrays.stream(DEFAULT_EDGE_PAIRS)
                .map(pair -> {
                    RoadNode fromNode = nodeMap.get(pair[0]);
                    RoadNode toNode = nodeMap.get(pair[1]);
                    return RoadEdge.builder()
                            .fromNodeId(fromNode.getId())
                            .toNodeId(toNode.getId())
                            .bidirectional(true)
                            .weight(fromNode.getPosition().distanceTo(toNode.getPosition()))
                            .build();
                })
                .toList();

        roadEdgeRepository.saveAll(edges);
        roadNetworkVersionRepository.save(RoadNetworkVersion.builder()
                .versionNo(1)
                .snapshot(objectMapper.writeValueAsString(Map.of(
                        "nodes", savedNodes.stream().map(this::toSnapshotNode).toList(),
                        "edges", edges.stream().map(this::toSnapshotEdge).toList()
                )))
                .createdAt(Instant.now())
                .build());
    }

    private void initializeDepots() {
        if (!depotRepository.findAll().isEmpty()) {
            return;
        }

        depotRepository.saveAll(DEFAULT_DEPOTS.stream()
                .map(template -> Depot.builder()
                        .name(template.getName())
                        .position(point(template.getPosition().getX(), template.getPosition().getY()))
                        .icon(template.getIcon())
                        .metadata(template.getMetadata())
                        .createdAt(Instant.now())
                        .build())
                .toList());
    }

    private List<Vehicle> createDefaultFleet() {
        return List.of(
                createVehicle(DEFAULT_SPAWN_POINTS.get(0), 92, 0.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(1), 88, 15.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(2), 95, 0.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(3), 84, 90.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(4), 86, 180.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(5), 90, 180.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(6), 89, 0.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(7), 87, 0.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(8), 85, 180.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(9), 94, 270.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(10), 91, 180.0),
                createVehicle(DEFAULT_SPAWN_POINTS.get(11), 83, 90.0)
        );
    }

    private Vehicle createVehicle(Position position, int battery, double heading) {
        return Vehicle.builder()
                .status(VehicleStatus.IDLE)
                .currentPosition(position)
                .battery(battery)
                .speed(8.0)
                .maxSpeed(12.0)
                .currentLoad(0.0)
                .capacity(80.0)
                .heading(heading)
                .totalTasks(0)
                .totalDistance(0.0)
                .orderQueueJson("[]")
                .loadingTimeRemaining(0)
                .build();
    }

    private boolean shouldRelocateLegacyFleet(List<Vehicle> vehicles) {
        if (vehicles.size() > DEFAULT_SPAWN_POINTS.size()) {
            return false;
        }

        return vehicles.stream().allMatch(vehicle -> {
            Position position = vehicle.getCurrentPosition();
            boolean isLegacyPosition = position != null && position.getX() <= 150 && position.getY() <= 120;
            boolean isUnassigned = vehicle.getCurrentOrderId() == null;
            boolean isUntouched = vehicle.getTotalTasks() == null || vehicle.getTotalTasks() == 0;
            return isLegacyPosition && isUnassigned && isUntouched;
        });
    }

    private void relocateLegacyFleet(List<Vehicle> vehicles) {
        for (int index = 0; index < vehicles.size(); index++) {
            Vehicle vehicle = vehicles.get(index);
            Position spawnPoint = DEFAULT_SPAWN_POINTS.get(index);
            vehicle.setCurrentPosition(spawnPoint);
            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setCurrentOrderId(null);
            vehicle.setHeading(index % 2 == 0 ? 0.0 : 180.0);
            if (vehicle.getBattery() == null || vehicle.getBattery() < 75) {
                vehicle.setBattery(85);
            }
            if (vehicle.getSpeed() == null || vehicle.getSpeed() < 6.0) {
                vehicle.setSpeed(8.0);
            }
            if (vehicle.getMaxSpeed() == null || vehicle.getMaxSpeed() < 10.0) {
                vehicle.setMaxSpeed(12.0);
            }
            if (vehicle.getCapacity() == null || vehicle.getCapacity() < 50.0) {
                vehicle.setCapacity(80.0);
            }
            if (vehicle.getCurrentLoad() == null) {
                vehicle.setCurrentLoad(0.0);
            }
            if (vehicle.getTotalDistance() == null) {
                vehicle.setTotalDistance(0.0);
            }
            if (vehicle.getOrderQueueJson() == null || vehicle.getOrderQueueJson().isBlank()) {
                vehicle.setOrderQueue(List.of());
            }
            if (vehicle.getLoadingTimeRemaining() == null || vehicle.getLoadingTimeRemaining() < 0) {
                vehicle.setLoadingTimeRemaining(0);
            }
        }
    }

    private static Position point(double x, double y) {
        return Position.builder().x(x).y(y).build();
    }

    private static RoadNode roadNode(double x, double y, String type, String metadata) {
        return RoadNode.builder()
                .position(point(x, y))
                .type(type)
                .metadata(metadata)
                .build();
    }

    private static Depot depot(String name, double x, double y, String icon, String metadata) {
        return Depot.builder()
                .name(name)
                .position(point(x, y))
                .icon(icon)
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();
    }

    private Map<String, Object> toSnapshotNode(RoadNode node) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", node.getId());
        snapshot.put("position", Map.of("x", node.getPosition().getX(), "y", node.getPosition().getY()));
        snapshot.put("type", node.getType());
        snapshot.put("metadata", node.getMetadata());
        return snapshot;
    }

    private Map<String, Object> toSnapshotEdge(RoadEdge edge) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", edge.getId());
        snapshot.put("fromNodeId", edge.getFromNodeId());
        snapshot.put("toNodeId", edge.getToNodeId());
        snapshot.put("bidirectional", edge.isBidirectional());
        snapshot.put("weight", edge.getWeight());
        snapshot.put("metadata", edge.getMetadata());
        return snapshot;
    }
}
