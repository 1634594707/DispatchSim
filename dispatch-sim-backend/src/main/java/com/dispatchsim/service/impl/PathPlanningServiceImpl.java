package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.StateException;
import com.dispatchsim.common.exception.TimeoutException;
import com.dispatchsim.common.retry.RetryExecutor;
import com.dispatchsim.domain.model.RoadEdge;
import com.dispatchsim.domain.model.RoadNode;
import com.dispatchsim.domain.repository.RoadEdgeRepository;
import com.dispatchsim.domain.repository.RoadNodeRepository;
import com.dispatchsim.dto.roadnetwork.RoadNetworkPathDto;
import com.dispatchsim.service.PathPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PathPlanningServiceImpl implements PathPlanningService {

    private static final long TIMEOUT_MS = 5_000L;

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final RetryExecutor retryExecutor;

    @Override
    public RoadNetworkPathDto findPath(Long startNodeId, Long endNodeId) {
        RoadNode startNode = roadNodeRepository.findById(startNodeId)
                .orElseThrow(() -> new ResourceNotFoundException("起点节点不存在: " + startNodeId));
        RoadNode endNode = roadNodeRepository.findById(endNodeId)
                .orElseThrow(() -> new ResourceNotFoundException("终点节点不存在: " + endNodeId));

        return retryExecutor.execute(
                "路径规划",
                () -> doFindPath(startNode, endNode),
                Set.of(TimeoutException.class)
        );
    }

    private RoadNetworkPathDto doFindPath(RoadNode startNode, RoadNode endNode) {
        long startedAt = System.currentTimeMillis();
        Map<Long, List<Neighbor>> graph = buildGraph();
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));

        for (RoadNode node : roadNodeRepository.findAll()) {
            distances.put(node.getId(), Double.POSITIVE_INFINITY);
        }

        distances.put(startNode.getId(), 0.0);
        queue.offer(new NodeDistance(startNode.getId(), 0.0));

        while (!queue.isEmpty()) {
            if (System.currentTimeMillis() - startedAt > TIMEOUT_MS) {
                throw new TimeoutException("路径规划超时，已超过 5 秒");
            }

            NodeDistance current = queue.poll();
            if (current.distance() > distances.getOrDefault(current.nodeId(), Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (current.nodeId().equals(endNode.getId())) {
                break;
            }

            for (Neighbor neighbor : graph.getOrDefault(current.nodeId(), List.of())) {
                double candidate = current.distance() + neighbor.weight();
                if (candidate < distances.getOrDefault(neighbor.nodeId(), Double.POSITIVE_INFINITY)) {
                    distances.put(neighbor.nodeId(), candidate);
                    previous.put(neighbor.nodeId(), current.nodeId());
                    queue.offer(new NodeDistance(neighbor.nodeId(), candidate));
                }
            }
        }

        double totalWeight = distances.getOrDefault(endNode.getId(), Double.POSITIVE_INFINITY);
        if (Double.isInfinite(totalWeight)) {
            throw new BusinessException("起点和终点之间不存在可达路径");
        }

        List<Long> path = rebuildPath(previous, startNode.getId(), endNode.getId());
        return new RoadNetworkPathDto(startNode.getId(), endNode.getId(), path, totalWeight);
    }

    private Map<Long, List<Neighbor>> buildGraph() {
        Map<Long, List<Neighbor>> graph = new HashMap<>();
        for (RoadEdge edge : roadEdgeRepository.findAll()) {
            graph.computeIfAbsent(edge.getFromNodeId(), key -> new ArrayList<>())
                    .add(new Neighbor(edge.getToNodeId(), edge.getWeight()));
            if (edge.isBidirectional()) {
                graph.computeIfAbsent(edge.getToNodeId(), key -> new ArrayList<>())
                        .add(new Neighbor(edge.getFromNodeId(), edge.getWeight()));
            }
        }
        return graph;
    }

    private List<Long> rebuildPath(Map<Long, Long> previous, Long startNodeId, Long endNodeId) {
        List<Long> path = new ArrayList<>();
        Long current = endNodeId;
        while (current != null) {
            path.add(0, current);
            if (current.equals(startNodeId)) {
                return path;
            }
            current = previous.get(current);
        }
        throw new StateException("路径重建失败");
    }

    private record Neighbor(Long nodeId, double weight) {
    }

    private record NodeDistance(Long nodeId, double distance) {
    }
}
