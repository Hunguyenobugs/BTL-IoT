package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.*;
import com.example.iotdashboard.repository.SensorRepository;
import com.example.iotdashboard.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final SensorRepository sensorRepository;

    /** GET /api/sensors/latest */
    @GetMapping("/latest")
    public ApiResponse<SensorLatestResponse> getLatest() {
        return ApiResponse.ok(sensorService.getLatest());
    }

    /** GET /api/sensors/chart?limit=15 */
    @GetMapping("/chart")
    public ApiResponse<SensorChartResponse> getChart(
            @RequestParam(defaultValue = "15") int limit) {
        return ApiResponse.ok(sensorService.getChartData(limit));
    }

    /**
     * GET /api/sensors
     * Params: type, time, value, sort (asc|desc), page (default 1), size (default 10)
     */
    @GetMapping
    public ApiResponse<PagedResponse<SensorDataItem>> getSensorList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String value,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        return ApiResponse.ok(sensorService.getSensorList(type, time, value, sort, page, size));
    }
}
