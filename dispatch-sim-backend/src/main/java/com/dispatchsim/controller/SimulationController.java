package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.simulation.BatchOrderRequest;
import com.dispatchsim.dto.simulation.BatchOrderResponse;
import com.dispatchsim.dto.simulation.SimulationStatusDto;
import com.dispatchsim.dto.simulation.UpdateSimulationSpeedRequest;
import com.dispatchsim.dto.simulation.UpdateStrategyRequest;
import com.dispatchsim.service.BatchOrderService;
import com.dispatchsim.service.SimulationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SIMULATION)
@RequiredArgsConstructor
@Tag(name = "仿真控制", description = "启动、暂停、停止仿真并切换调度策略")
public class SimulationController {

    private final SimulationEngine simulationEngine;
    private final BatchOrderService batchOrderService;

    @PostMapping("/start")
    @Operation(summary = "启动仿真")
    public ApiResponse<SimulationStatusDto> start() {
        return ApiResponse.success(simulationEngine.start());
    }

    @PostMapping("/stop")
    @Operation(summary = "停止仿真")
    public ApiResponse<SimulationStatusDto> stop() {
        return ApiResponse.success(simulationEngine.stop());
    }

    @PostMapping("/pause")
    @Operation(summary = "暂停仿真")
    public ApiResponse<SimulationStatusDto> pause() {
        return ApiResponse.success(simulationEngine.pause());
    }

    @PostMapping("/resume")
    @Operation(summary = "恢复仿真")
    public ApiResponse<SimulationStatusDto> resume() {
        return ApiResponse.success(simulationEngine.resume());
    }

    @PostMapping("/tick")
    @Operation(summary = "单步执行")
    public ApiResponse<SimulationStatusDto> tick() {
        return ApiResponse.success(simulationEngine.tick());
    }

    @PostMapping("/step")
    @Operation(summary = "执行单步仿真", description = "自动进入暂停态并只推进一个 tick")
    public ApiResponse<SimulationStatusDto> step() {
        return ApiResponse.success(simulationEngine.executeStep());
    }

    @PostMapping("/reset")
    @Operation(summary = "重置仿真", description = "清空订单、重置车辆与统计，但保留出货点和路网")
    public ApiResponse<SimulationStatusDto> reset() {
        return ApiResponse.success(simulationEngine.reset());
    }

    @PutMapping("/speed")
    @Operation(summary = "更新仿真速度")
    public ApiResponse<SimulationStatusDto> updateSpeed(@Valid @RequestBody UpdateSimulationSpeedRequest request) {
        return ApiResponse.success(simulationEngine.updateSpeed(request));
    }

    @PostMapping("/strategy")
    @Operation(summary = "切换调度策略")
    public ApiResponse<SimulationStatusDto> updateStrategy(@Valid @RequestBody UpdateStrategyRequest request) {
        return ApiResponse.success(simulationEngine.updateStrategy(request.strategy()));
    }

    @PostMapping("/batch-orders")
    @Operation(summary = "批量生成订单", description = "按指定策略生成一批订单，并立即触发常规创建与调度流程")
    public ApiResponse<BatchOrderResponse> batchOrders(@Valid @RequestBody BatchOrderRequest request) {
        return ApiResponse.success(batchOrderService.generate(request));
    }

    @GetMapping("/status")
    @Operation(summary = "查询仿真状态")
    public ApiResponse<SimulationStatusDto> getStatus() {
        return ApiResponse.success(simulationEngine.getStatus());
    }
}
