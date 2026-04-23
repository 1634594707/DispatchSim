package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.ValidationException;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.model.RoadEdge;
import com.dispatchsim.domain.model.RoadNetworkVersion;
import com.dispatchsim.domain.model.RoadNode;
import com.dispatchsim.domain.repository.RoadEdgeRepository;
import com.dispatchsim.domain.repository.RoadNetworkVersionRepository;
import com.dispatchsim.domain.repository.RoadNodeRepository;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.roadnetwork.BulkRoadNetworkRequest;
import com.dispatchsim.dto.roadnetwork.RoadEdgeDto;
import com.dispatchsim.dto.roadnetwork.RoadEdgeRequest;
import com.dispatchsim.dto.roadnetwork.RoadNetworkDto;
import com.dispatchsim.dto.roadnetwork.RoadNetworkVersionDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeRequest;
import com.dispatchsim.service.RoadNetworkService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadNetworkServiceImpl implements RoadNetworkService {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final RoadNetworkVersionRepository roadNetworkVersionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RoadNodeDto addNode(RoadNodeRequest request) {
        RoadNode node = roadNodeRepository.save(RoadNode.builder()
                .position(toPosition(request.position()))
                .type(request.type())
                .metadata(request.metadata())
                .build());
        recordVersionSnapshot();
        return toDto(node);
    }

    @Override
    @Transactional
    public RoadEdgeDto addEdge(RoadEdgeRequest request) {
        RoadNode fromNode = findNodeEntity(request.fromNodeId());
        RoadNode toNode = findNodeEntity(request.toNodeId());
        validateDuplicateEdge(request.fromNodeId(), request.toNodeId());

        double weight = request.weight() != null
                ? request.weight()
                : fromNode.getPosition().distanceTo(toNode.getPosition());

        RoadEdge edge = roadEdgeRepository.save(RoadEdge.builder()
                .fromNodeId(request.fromNodeId())
                .toNodeId(request.toNodeId())
                .bidirectional(request.bidirectional())
                .weight(weight)
                .metadata(request.metadata())
                .build());
        recordVersionSnapshot();
        return toDto(edge);
    }

    @Override
    @Transactional
    public RoadNodeDto updateNode(Long id, RoadNodeRequest request) {
        RoadNode node = findNodeEntity(id);
        node.setPosition(toPosition(request.position()));
        node.setType(request.type());
        node.setMetadata(request.metadata());
        RoadNode savedNode = roadNodeRepository.save(node);

        List<RoadEdge> connectedEdges = roadEdgeRepository.findByFromNodeIdOrToNodeId(id, id);
        for (RoadEdge edge : connectedEdges) {
            RoadNode fromNode = findNodeEntity(edge.getFromNodeId());
            RoadNode toNode = findNodeEntity(edge.getToNodeId());
            edge.setWeight(fromNode.getPosition().distanceTo(toNode.getPosition()));
        }
        roadEdgeRepository.saveAll(connectedEdges);

        recordVersionSnapshot();
        return toDto(savedNode);
    }

    @Override
    @Transactional
    public void deleteNode(Long id) {
        findNodeEntity(id);
        roadEdgeRepository.deleteAll(roadEdgeRepository.findByFromNodeIdOrToNodeId(id, id));
        roadNodeRepository.deleteById(id);
        recordVersionSnapshot();
    }

    @Override
    @Transactional
    public void deleteEdge(Long id) {
        if (!roadEdgeRepository.existsById(id)) {
            throw new ResourceNotFoundException("路网边不存在: " + id);
        }
        roadEdgeRepository.deleteById(id);
        recordVersionSnapshot();
    }

    @Override
    public List<RoadNodeDto> findAllNodes() {
        return roadNodeRepository.findAll().stream()
                .sorted(Comparator.comparing(RoadNode::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<RoadEdgeDto> findAllEdges() {
        return roadEdgeRepository.findAll().stream()
                .sorted(Comparator.comparing(RoadEdge::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public RoadNetworkDto getNetwork() {
        RoadNetworkVersion latestVersion = roadNetworkVersionRepository.findTopByOrderByVersionNoDesc().orElse(null);
        int version = latestVersion == null ? 1 : latestVersion.getVersionNo();
        Instant updatedAt = latestVersion == null ? Instant.now() : latestVersion.getCreatedAt();
        return new RoadNetworkDto(findAllNodes(), findAllEdges(), version, updatedAt);
    }

    @Override
    @Transactional
    public RoadNetworkDto replaceNetwork(BulkRoadNetworkRequest request) {
        roadEdgeRepository.deleteAllInBatch();
        roadNodeRepository.deleteAllInBatch();

        Map<Long, Long> nodeIdMap = new HashMap<>();
        for (RoadNodeDto nodeDto : request.nodes()) {
            RoadNode savedNode = roadNodeRepository.save(RoadNode.builder()
                    .position(toPosition(nodeDto.position()))
                    .type(nodeDto.type())
                    .metadata(nodeDto.metadata())
                    .build());
            if (nodeDto.id() != null) {
                nodeIdMap.put(nodeDto.id(), savedNode.getId());
            }
        }

        for (RoadEdgeDto edgeDto : request.edges()) {
            Long fromNodeId = nodeIdMap.getOrDefault(edgeDto.fromNodeId(), edgeDto.fromNodeId());
            Long toNodeId = nodeIdMap.getOrDefault(edgeDto.toNodeId(), edgeDto.toNodeId());
            RoadNode fromNode = findNodeEntity(fromNodeId);
            RoadNode toNode = findNodeEntity(toNodeId);
            roadEdgeRepository.save(RoadEdge.builder()
                    .fromNodeId(fromNodeId)
                    .toNodeId(toNodeId)
                    .bidirectional(edgeDto.bidirectional())
                    .weight(edgeDto.weight() > 0 ? edgeDto.weight() : fromNode.getPosition().distanceTo(toNode.getPosition()))
                    .metadata(edgeDto.metadata())
                    .build());
        }

        recordVersionSnapshot();
        return getNetwork();
    }

    @Override
    @Transactional
    public RoadNetworkDto importGeoJson(JsonNode geoJson) {
        if (geoJson == null || !"FeatureCollection".equals(geoJson.path("type").asText()) || !geoJson.has("features")) {
            throw new ValidationException("GeoJSON 必须是 FeatureCollection");
        }

        List<RoadNodeDto> nodes = new ArrayList<>();
        List<RoadEdgeDto> edges = new ArrayList<>();
        Map<Long, Long> originalNodeIds = new HashMap<>();

        long tempNodeId = 1;
        long tempEdgeId = 1;
        for (JsonNode feature : geoJson.path("features")) {
            if (!"Point".equals(feature.path("geometry").path("type").asText())) {
                continue;
            }
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            JsonNode properties = feature.path("properties");
            long originalId = properties.has("id") ? properties.path("id").asLong() : tempNodeId;
            originalNodeIds.put(originalId, tempNodeId);
            nodes.add(new RoadNodeDto(
                    tempNodeId++,
                    new PositionDto(coordinates.get(0).asDouble(), coordinates.get(1).asDouble()),
                    properties.path("type").asText("intersection"),
                    toCompactJson(properties)
            ));
        }

        for (JsonNode feature : geoJson.path("features")) {
            if (!"LineString".equals(feature.path("geometry").path("type").asText())) {
                continue;
            }
            JsonNode properties = feature.path("properties");
            long fromNodeId = properties.path("fromNodeId").asLong();
            long toNodeId = properties.path("toNodeId").asLong();
            edges.add(new RoadEdgeDto(
                    tempEdgeId++,
                    originalNodeIds.getOrDefault(fromNodeId, fromNodeId),
                    originalNodeIds.getOrDefault(toNodeId, toNodeId),
                    properties.path("bidirectional").asBoolean(true),
                    properties.path("weight").asDouble(0.0),
                    toCompactJson(properties)
            ));
        }

        return replaceNetwork(new BulkRoadNetworkRequest(nodes, edges));
    }

    @Override
    public JsonNode exportGeoJson() {
        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        ArrayNode features = objectMapper.createArrayNode();

        for (RoadNode node : roadNodeRepository.findAll().stream().sorted(Comparator.comparing(RoadNode::getId)).toList()) {
            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");
            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Point");
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(node.getPosition().getX());
            coordinates.add(node.getPosition().getY());

            ObjectNode properties = feature.putObject("properties");
            properties.put("id", node.getId());
            properties.put("type", node.getType());
            mergeMetadata(properties, node.getMetadata());
            features.add(feature);
        }

        for (RoadEdge edge : roadEdgeRepository.findAll().stream().sorted(Comparator.comparing(RoadEdge::getId)).toList()) {
            RoadNode fromNode = findNodeEntity(edge.getFromNodeId());
            RoadNode toNode = findNodeEntity(edge.getToNodeId());
            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");
            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "LineString");
            ArrayNode coordinates = geometry.putArray("coordinates");
            ArrayNode fromCoordinates = objectMapper.createArrayNode();
            fromCoordinates.add(fromNode.getPosition().getX());
            fromCoordinates.add(fromNode.getPosition().getY());
            ArrayNode toCoordinates = objectMapper.createArrayNode();
            toCoordinates.add(toNode.getPosition().getX());
            toCoordinates.add(toNode.getPosition().getY());
            coordinates.add(fromCoordinates);
            coordinates.add(toCoordinates);

            ObjectNode properties = feature.putObject("properties");
            properties.put("id", edge.getId());
            properties.put("fromNodeId", edge.getFromNodeId());
            properties.put("toNodeId", edge.getToNodeId());
            properties.put("bidirectional", edge.isBidirectional());
            properties.put("weight", edge.getWeight());
            mergeMetadata(properties, edge.getMetadata());
            features.add(feature);
        }

        featureCollection.set("features", features);
        return featureCollection;
    }

    @Override
    public List<RoadNetworkVersionDto> listVersions() {
        return roadNetworkVersionRepository.findAll().stream()
                .sorted(Comparator.comparing(RoadNetworkVersion::getVersionNo))
                .map(this::toDto)
                .toList();
    }

    @Override
    public RoadNetworkVersionDto getVersion(Long id) {
        RoadNetworkVersion version = roadNetworkVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("路网版本不存在: " + id));
        return toDto(version);
    }

    private void validateDuplicateEdge(Long fromNodeId, Long toNodeId) {
        boolean exists = roadEdgeRepository.findAll().stream().anyMatch(edge ->
                (edge.getFromNodeId().equals(fromNodeId) && edge.getToNodeId().equals(toNodeId)) ||
                        (edge.isBidirectional() && edge.getFromNodeId().equals(toNodeId) && edge.getToNodeId().equals(fromNodeId))
        );
        if (exists) {
            throw new BusinessException("节点之间已存在道路边");
        }
    }

    private RoadNode findNodeEntity(Long id) {
        return roadNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("路网节点不存在: " + id));
    }

    private void recordVersionSnapshot() {
        try {
            RoadNetworkDto network = new RoadNetworkDto(findAllNodes(), findAllEdges(), 0, Instant.now());
            int nextVersion = roadNetworkVersionRepository.findTopByOrderByVersionNoDesc()
                    .map(version -> version.getVersionNo() + 1)
                    .orElse(1);
            roadNetworkVersionRepository.save(RoadNetworkVersion.builder()
                    .versionNo(nextVersion)
                    .snapshot(objectMapper.writeValueAsString(network))
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception exception) {
            throw new BusinessException("路网版本快照保存失败");
        }
    }

    private String toCompactJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new BusinessException("元数据序列化失败");
        }
    }

    private void mergeMetadata(ObjectNode target, String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return;
        }
        try {
            JsonNode metadataNode = objectMapper.readTree(metadata);
            if (metadataNode.isObject()) {
                target.setAll((ObjectNode) metadataNode);
            }
        } catch (Exception ignored) {
            target.put("metadata", metadata);
        }
    }

    private RoadNodeDto toDto(RoadNode node) {
        return new RoadNodeDto(
                node.getId(),
                new PositionDto(node.getPosition().getX(), node.getPosition().getY()),
                node.getType(),
                node.getMetadata()
        );
    }

    private RoadEdgeDto toDto(RoadEdge edge) {
        return new RoadEdgeDto(
                edge.getId(),
                edge.getFromNodeId(),
                edge.getToNodeId(),
                edge.isBidirectional(),
                edge.getWeight(),
                edge.getMetadata()
        );
    }

    private RoadNetworkVersionDto toDto(RoadNetworkVersion version) {
        return new RoadNetworkVersionDto(
                version.getId(),
                version.getVersionNo(),
                version.getSnapshot(),
                version.getCreatedAt()
        );
    }

    private Position toPosition(PositionDto position) {
        return Position.builder()
                .x(position.x())
                .y(position.y())
                .build();
    }
}
