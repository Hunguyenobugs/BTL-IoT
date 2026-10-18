package com.example.iotdashboard.service;

import com.example.iotdashboard.entity.Devices;
import com.example.iotdashboard.entity.History;
import com.example.iotdashboard.repository.DeviceRepository;
import com.example.iotdashboard.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job: quét các bản ghi History đang ở trạng thái WAITING
 * đã vượt quá thời gian timeout, tự động cập nhật thành FAILED
 * và khôi phục trạng thái thiết bị trong CSDL (tránh kẹt WAITING).
 *
 * Chạy mỗi 2 giây (fixedRate = 2000ms).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTimeoutChecker {

    private final HistoryRepository historyRepository;
    private final DeviceRepository deviceRepository;

    @Value("${device.control.timeout-seconds:10}")
    private long timeoutSeconds;

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void checkTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        LocalDateTime now = LocalDateTime.now();

        // 1. Quét các request WAITING đã quá hạn
        List<History> timedOutList = historyRepository.findByStatusAndExecutedAtBefore("WAITING", cutoff);
        for (History h : timedOutList) {
            h.setStatus("FAILED");
            h.setExecutedAt(now);
            historyRepository.save(h);

            Devices d = h.getDevice();
            if (d != null) {
                // Nếu lệnh gửi đi là OFF mà lỗi (timeout) -> đèn thực tế vẫn đang BẬT (ON)
                // Nếu lệnh gửi đi là ON mà lỗi (timeout) -> đèn thực tế vẫn đang TẮT (OFF)
                String actualStatus = "OFF".equalsIgnoreCase(h.getAction()) ? "ON" : "OFF";
                d.setCurrentStatus(actualStatus);
                deviceRepository.save(d);
                log.warn("Device {} timed out on action {}. Restored current_status to {}",
                        d.getId(), h.getAction(), actualStatus);
            }
        }

        if (!timedOutList.isEmpty()) {
            log.info("DeviceTimeoutChecker: marked {} WAITING record(s) as FAILED (timeout={}s)",
                    timedOutList.size(), timeoutSeconds);
        }

        // 2. Dọn dẹp thiết bị bị kẹt WAITING mồ côi (không còn request WAITING nào)
        List<Devices> stuckDevices = deviceRepository.findByCurrentStatus("WAITING");
        for (Devices d : stuckDevices) {
            boolean hasWaiting = historyRepository.existsByDeviceIdAndStatus(d.getId(), "WAITING");
            if (!hasWaiting) {
                String restoredStatus = resolveLastConfirmedStatus(d.getId());
                d.setCurrentStatus(restoredStatus);
                deviceRepository.save(d);
                log.info("Reset stuck device {}: WAITING → {}", d.getId(), restoredStatus);
            }
        }
    }

    /**
     * Xác định trạng thái thực tế gần nhất của thiết bị dựa trên lần điều khiển thành công gần nhất.
     * Nếu không có lịch sử thành công, mặc định là OFF.
     */
    private String resolveLastConfirmedStatus(Long deviceId) {
        return historyRepository
                .findTopByDeviceIdAndStatusOrderByIdDesc(deviceId, "SUCCESS")
                .map(History::getAction)
                .orElseGet(() -> historyRepository
                        .findTopByDeviceIdAndStatusOrderByIdDesc(deviceId, "ON")
                        .map(History::getAction)
                        .orElse("OFF"));
    }
}
