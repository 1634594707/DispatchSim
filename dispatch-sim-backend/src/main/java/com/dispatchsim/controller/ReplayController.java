package com.dispatchsim.controller;

import com.dispatchsim.common.constants.ApiPaths;
import com.dispatchsim.dto.ApiResponse;
import com.dispatchsim.dto.simulation.ReplayControlRequest;
import com.dispatchsim.dto.simulation.ReplayEventDto;
import com.dispatchsim.dto.simulation.ReplayFrameDto;
import com.dispatchsim.dto.simulation.ReplaySessionDto;
import com.dispatchsim.service.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.SIMULATION_REPLAY)
@RequiredArgsConstructor
@Tag(name = "历史回放", description = "查询仿真历史事件并生成回放帧")
public class ReplayController {

    private final ReplayService replayService;

    @GetMapping("/sessions")
    @Operation(summary = "查询历史会话列表")
    public ApiResponse<List<ReplaySessionDto>> listSessions() {
        return ApiResponse.success(replayService.listSessions());
    }

    @GetMapping("/sessions/{sessionId}/events")
    @Operation(summary = "按会话 ID 查询历史事件")
    public ApiResponse<List<ReplayEventDto>> getSessionEvents(@PathVariable String sessionId) {
        return ApiResponse.success(replayService.findBySessionId(sessionId));
    }

    @GetMapping("/events")
    @Operation(summary = "按时间范围查询历史事件")
    public ApiResponse<List<ReplayEventDto>> getEventsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.success(replayService.findByTimeRange(startTime, endTime));
    }

    @PostMapping("/control")
    @Operation(summary = "生成回放控制帧", description = "支持播放、暂停、拖动、速度调节和单步模式")
    public ApiResponse<ReplayFrameDto> control(@Valid @RequestBody ReplayControlRequest request) {
        return ApiResponse.success(replayService.buildFrame(
                request.sessionId(),
                request.action(),
                request.progress(),
                request.speed(),
                request.step()
        ));
    }
}
