package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body cho POST /api/devices/{deviceId}/control */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceControlRequest {
    /** ON | OFF */
    private String action;
}
