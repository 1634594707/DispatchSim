package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.roadnetwork.BulkRoadNetworkRequest;
import com.dispatchsim.dto.roadnetwork.RoadEdgeDto;
import com.dispatchsim.dto.roadnetwork.RoadEdgeRequest;
import com.dispatchsim.dto.roadnetwork.RoadNetworkDto;
import com.dispatchsim.dto.roadnetwork.RoadNetworkPathDto;
import com.dispatchsim.dto.roadnetwork.RoadNetworkVersionDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeDto;
import com.dispatchsim.dto.roadnetwork.RoadNodeRequest;
import com.dispatchsim.service.PathPlanningService;
import com.dispatchsim.service.RoadNetworkService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ROAD_NETWORK)
@RequiredArgsConstructor
@Tag(name = "路网管理", description = "路网节点、道路边、GeoJSON 与路径规划")
public class RoadNetworkController {

    private final RoadNetworkService roadNetworkService;
    private final PathPlanningService pathPlanningService;

    @PostMapping("/nodes")
    @Operation(summary = "创建路网节点")
    public ApiResponse<RoadNodeDto> createNode(@Valid @RequestBody RoadNodeRequest request) {
        return ApiResponse.success(roadNetworkService.addNode(request));
    }

    @PutMapping("/nodes/{id}")
    @Operation(summary = "更新路网节点")
    public ApiResponse<RoadNodeDto> updateNode(
            @Parameter(description = "节点 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody RoadNodeRequest request) {
        return ApiResponse.success(roadNetworkService.updateNode(id, request));
    }

    @DeleteMapping("/nodes/{id}")
    @Operation(summary = "删除路网节点")
    public ApiResponse<Void> deleteNode(@Parameter(description = "节点 ID", example = "1") @PathVariable Long id) {
        roadNetworkService.deleteNode(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/edges")
    @Operation(summary = "创建路网边")
    public ApiResponse<RoadEdgeDto> createEdge(@Valid @RequestBody RoadEdgeRequest request) {
        return ApiResponse.success(roadNetworkService.addEdge(request));
    }

    @DeleteMapping("/edges/{id}")
    @Operation(summary = "删除路网边")
    public ApiResponse<Void> deleteEdge(@Parameter(description = "边 ID", example = "1") @PathVariable Long id) {
        roadNetworkService.deleteEdge(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping
    @Operation(summary = "获取完整路网")
    public ApiResponse<RoadNetworkDto> getNetwork() {
        return ApiResponse.success(roadNetworkService.getNetwork());
    }

    @PostMapping
    @Operation(summary = "整体更新路网")
    public ApiResponse<RoadNetworkDto> replaceNetwork(@Valid @RequestBody BulkRoadNetworkRequest request) {
        return ApiResponse.success(roadNetworkService.replaceNetwork(request));
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "从 GeoJSON 导入路网")
    public ApiResponse<RoadNetworkDto> importGeoJson(@RequestBody JsonNode geoJson) {
        return ApiResponse.success(roadNetworkService.importGeoJson(geoJson));
    }

    @GetMapping(path = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "导出路网为 GeoJSON")
    public JsonNode exportGeoJson() {
        return roadNetworkService.exportGeoJson();
    }

    @GetMapping("/path")
    @Operation(summary = "计算最短路径")
    public ApiResponse<RoadNetworkPathDto> findPath(
            @RequestParam Long startNodeId,
            @RequestParam Long endNodeId) {
        return ApiResponse.success(pathPlanningService.findPath(startNodeId, endNodeId));
    }

    @GetMapping("/versions")
    @Operation(summary = "查询路网版本列表")
    public ApiResponse<List<RoadNetworkVersionDto>> listVersions() {
        return ApiResponse.success(roadNetworkService.listVersions());
    }

    @GetMapping("/versions/{id}")
    @Operation(summary = "查询路网版本详情")
    public ApiResponse<RoadNetworkVersionDto> getVersion(@PathVariable Long id) {
        return ApiResponse.success(roadNetworkService.getVersion(id));
    }
}
