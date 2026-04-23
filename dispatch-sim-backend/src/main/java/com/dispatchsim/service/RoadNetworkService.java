package com.dispatchsim.service;

import com.dispatchsim.dto.roadnetwork.BulkRoadNetworkRequest;
import com.dispatchsim.dto.roadnetwork.RoadEdgeDto;
import com.dispatchsim.dto.roadnetwork.RoadEdgeRequest;
import com.dispatchsim.dto.roadnetwork.RoadNetworkDto;
import com.dispatchsim.dto.roadnetwork.RoadNetworkVersionDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface RoadNetworkService {

    RoadNodeDto addNode(RoadNodeRequest request);

    RoadEdgeDto addEdge(RoadEdgeRequest request);

    RoadNodeDto updateNode(Long id, RoadNodeRequest request);

    void deleteNode(Long id);

    void deleteEdge(Long id);

    List<RoadNodeDto> findAllNodes();

    List<RoadEdgeDto> findAllEdges();

    RoadNetworkDto getNetwork();

    RoadNetworkDto replaceNetwork(BulkRoadNetworkRequest request);

    RoadNetworkDto importGeoJson(JsonNode geoJson);

    JsonNode exportGeoJson();

    List<RoadNetworkVersionDto> listVersions();

    RoadNetworkVersionDto getVersion(Long id);
}
