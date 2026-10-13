package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Item trong lịch sử điều khiển (GET /api/devices/history) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {
    private Long id;
    private String deviceName;
    private String action;
    private String status;   // WAITING | ON | OFF | FAILED
    private String username; // Tên người dùng thực hiện lệnh
    private String time;
}
