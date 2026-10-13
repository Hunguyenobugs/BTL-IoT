package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Trạng thái 1 thiết bị cho GET /api/devices/status */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusDto {
    private Long deviceId;
    private String deviceName;
    private String status;  // ON | OFF | WAITING
}
