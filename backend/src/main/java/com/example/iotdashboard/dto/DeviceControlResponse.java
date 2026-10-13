package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response cho POST /api/devices/{deviceId}/control */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceControlResponse {
    private Long historyId;
    private Long deviceId;
    private String action;
    private String status;  // WAITING
    private String time;
}
