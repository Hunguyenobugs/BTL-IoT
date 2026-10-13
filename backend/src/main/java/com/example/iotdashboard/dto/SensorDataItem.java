package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Item trong danh sách lịch sử cảm biến (GET /api/sensors) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataItem {
    private Long id;
    private String sensorType;
    private String value;   // "23°C", "60%", "450 lux"
    private String time;
}
