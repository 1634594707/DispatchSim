package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.dto.depot.DepotDto;
import com.dispatchsim.dto.depot.DepotUpsertRequest;
import com.dispatchsim.service.DepotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.DEPOTS)
@RequiredArgsConstructor
@Tag(name = "配送中心管理", description = "配送中心的创建、查询、更新与删除")
public class DepotController {

    private final DepotService depotService;

    @PostMapping
    @Operation(summary = "创建配送中心")
    public ApiResponse<DepotDto> createDepot(@Valid @RequestBody DepotUpsertRequest request) {
        return ApiResponse.success(depotService.createDepot(request));
    }

    @GetMapping
    @Operation(summary = "查询配送中心列表")
    public ApiResponse<PageResponse<DepotDto>> listDepots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(depotService.findDepots(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询配送中心详情")
    public ApiResponse<DepotDto> getDepot(@Parameter(description = "配送中心 ID", example = "1") @PathVariable Long id) {
        return ApiResponse.success(depotService.getDepot(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新配送中心")
    public ApiResponse<DepotDto> updateDepot(
            @Parameter(description = "配送中心 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody DepotUpsertRequest request) {
        return ApiResponse.success(depotService.updateDepot(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配送中心")
    public ApiResponse<Void> deleteDepot(@Parameter(description = "配送中心 ID", example = "1") @PathVariable Long id) {
        depotService.deleteDepot(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/nearest")
    @Operation(summary = "查询最近配送中心")
    public ApiResponse<DepotDto> findNearestDepot(
            @RequestParam double x,
            @RequestParam double y) {
        return ApiResponse.success(depotService.findNearestDepot(x, y));
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入配送中心 CSV")
    public ApiResponse<List<DepotDto>> importDepots(@RequestPart("file") MultipartFile file) throws IOException {
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ApiResponse.success(depotService.importDepots(csvContent));
    }

    @GetMapping(path = "/export", produces = "text/csv")
    @Operation(summary = "导出配送中心 CSV")
    public ResponseEntity<String> exportDepots() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=depots.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(depotService.exportDepots());
    }
}
