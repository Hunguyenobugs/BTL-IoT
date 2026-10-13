package com.example.iotdashboard.service;

import com.example.iotdashboard.dto.*;
import com.example.iotdashboard.entity.Devices;
import com.example.iotdashboard.entity.History;
import com.example.iotdashboard.entity.User;
import com.example.iotdashboard.mqtt.MqttGateway;
import com.example.iotdashboard.repository.DeviceRepository;
import com.example.iotdashboard.repository.HistoryRepository;
import com.example.iotdashboard.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final HistoryRepository historyRepository;
    private final MqttGateway mqttGateway;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ─── API: GET /api/devices/status ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeviceStatusDto> getAllDeviceStatus() {
        return deviceRepository.findAll().stream()
                .map(d -> new DeviceStatusDto(d.getId(), d.getName(), d.getCurrentStatus()))
                .toList();
    }

    // ─── API: POST /api/devices/{deviceId}/control ──────────────────────────────

    @Transactional
    public DeviceControlResponse control(Long deviceId, String action) {
        Devices device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        // Validate action
        if (!"ON".equals(action) && !"OFF".equals(action)) {
            throw new IllegalArgumentException("Action must be ON or OFF");
        }

        // 1. Insert History với status WAITING
        // Lấy user đầu tiên trong hệ thống (hệ thống 1 user)
        User user = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No user found in database"));

        History history = new History();
        history.setUser(user);
        history.setDevice(device);
        history.setAction(action);
        history.setStatus("WAITING");
        history.setExecutedAt(LocalDateTime.now());
        history = historyRepository.save(history);

        // 2. Cập nhật current_status của device thành WAITING
        device.setCurrentStatus("WAITING");
        deviceRepository.save(device);

        // 3. Publish MQTT lệnh điều khiển
        String deviceKey = "led" + deviceId; // led1, led2
        mqttGateway.publishDeviceControl(deviceId, deviceKey, action);

        // 4. Trả response ngay (không block chờ ESP32)
        return new DeviceControlResponse(
                history.getId(),
                deviceId,
                action,
                "WAITING",
                history.getExecutedAt().format(ISO_FMT)
        );
    }

    // ─── API: GET /api/devices/history/{historyId}/status ───────────────────────

    @Transactional(readOnly = true)
    public HistoryStatusResponse getHistoryStatus(Long historyId) {
        History h = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + historyId));
        return new HistoryStatusResponse(h.getId(), h.getStatus());
    }

    // ─── MQTT: nhận device/status từ ESP32 ──────────────────────────────────────

    /**
     * Xử lý khi nhận message từ topic device/status.
     * Payload: { "deviceId": 1, "device": "led1", "status": "ON" }
     */
    @Transactional
    public void handleDeviceStatus(JsonNode json) {
        long deviceId = json.path("deviceId").asLong(-1);
        String status = json.path("status").asText("").toUpperCase();

        if (deviceId < 0 || status.isBlank()) {
            log.warn("Invalid device/status payload: {}", json);
            return;
        }

        // Chỉ chấp nhận ON hoặc OFF từ ESP32
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            log.warn("Unexpected status from ESP32: {}", status);
            return;
        }

        // Tìm bản ghi WAITING mới nhất của device này
        historyRepository.findTopByDeviceIdAndStatusOrderByIdDesc(deviceId, "WAITING")
                .ifPresentOrElse(h -> {
                    h.setStatus(status);
                    h.setExecutedAt(LocalDateTime.now()); // ghi đè = thời điểm resolve
                    historyRepository.save(h);
                    log.info("History {} resolved: WAITING → {}", h.getId(), status);
                }, () -> log.warn("No WAITING history found for deviceId={}", deviceId));

        // Cập nhật current_status của device
        deviceRepository.findById(deviceId).ifPresent(d -> {
            d.setCurrentStatus(status);
            deviceRepository.save(d);
            log.info("Device {} current_status updated to {}", deviceId, status);
        });
    }

    // ─── API: GET /api/devices/history ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<HistoryItem> getHistory(
            String deviceName, String time, String action, String status, String sort, int page, int size) {

        String dn = (deviceName != null && !deviceName.isBlank()) ? deviceName.trim() : null;
        String ts = (time != null && !time.isBlank()) ? time.trim() : null;
        String ac = (action != null && !action.isBlank()) ? action.toUpperCase().trim() : null;
        String st = (status != null && !status.isBlank()) ? status.toUpperCase().trim() : null;

        String sortParam = "asc".equalsIgnoreCase(sort) ? "asc" : "desc";
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<History> dbPage = historyRepository.findWithFilters(dn, ts, ac, st, sortParam, pageable);

        List<HistoryItem> items = dbPage.getContent().stream()
                .map(h -> new HistoryItem(
                        h.getId(),
                        h.getDevice().getName(),
                        h.getAction(),
                        h.getStatus(),
                        h.getUser() != null ? h.getUser().getName() : null,
                        h.getExecutedAt().format(ISO_FMT)
                ))
                .toList();

        return new PagedResponse<>(items, page, dbPage.getTotalPages(), dbPage.getTotalElements());
    }

    // ─── Inner DTO: history status polling ──────────────────────────────────────

    public record HistoryStatusResponse(Long historyId, String status) {}
}
