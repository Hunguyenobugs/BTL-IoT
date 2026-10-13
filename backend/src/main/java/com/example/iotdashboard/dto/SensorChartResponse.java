package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response cho GET /api/sensors/chart?limit=15 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorChartResponse {
    private List<String> labels;
    private List<Double> temperature;
    private List<Double> humidity;
    private List<Double> light;
}
