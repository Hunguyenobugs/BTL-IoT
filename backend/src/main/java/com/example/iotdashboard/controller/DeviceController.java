package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.*;
import com.example.iotdashboard.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /** GET /api/devices/status */
    @GetMapping("/status")
    public ApiResponse<List<DeviceStatusDto>> getDeviceStatus() {
        return ApiResponse.ok(deviceService.getAllDeviceStatus());
    }

    /**
     * POST /api/devices/{deviceId}/control
     * Body: { "action": "ON" }
     */
    @PostMapping("/{deviceId}/control")
    public ApiResponse<DeviceControlResponse> control(
            @PathVariable Long deviceId,
            @RequestBody DeviceControlRequest request) {
        return ApiResponse.ok(deviceService.control(deviceId, request.getAction()));
    }

    /** GET /api/devices/history/{historyId}/status — FE polling */
    @GetMapping("/history/{historyId}/status")
    public ApiResponse<DeviceService.HistoryStatusResponse> getHistoryStatus(
            @PathVariable Long historyId) {
        return ApiResponse.ok(deviceService.getHistoryStatus(historyId));
    }

    /**
     * GET /api/devices/history
     * Params: deviceName, time, action, status, sort, page, size
     */
    @GetMapping("/history")
    public ApiResponse<PagedResponse<HistoryItem>> getHistory(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        return ApiResponse.ok(deviceService.getHistory(deviceName, time, action, status, sort, page, size));
    }
}
