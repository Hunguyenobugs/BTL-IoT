package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response cho GET /api/sensors/latest */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorLatestResponse {
    private SensorValueDto temperature;
    private SensorValueDto humidity;
    private SensorValueDto light;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorValueDto {
        private Double value;
        private String unit;
        private String time;
    }
}
