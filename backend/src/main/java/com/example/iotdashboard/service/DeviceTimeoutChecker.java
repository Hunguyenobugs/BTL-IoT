package com.example.iotdashboard.service;

import com.example.iotdashboard.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job: quét các bản ghi History đang ở trạng thái WAITING
 * đã vượt quá thời gian timeout, tự động cập nhật thành FAILED.
 *
 * Chạy mỗi 2 giây (fixedRate = 2000ms).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTimeoutChecker {

    private final HistoryRepository historyRepository;

    @Value("${device.control.timeout-seconds:10}")
    private long timeoutSeconds;

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void checkTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        LocalDateTime now = LocalDateTime.now();

        int updated = historyRepository.markTimedOutAsFailed(cutoff, now);
        if (updated > 0) {
            log.info("DeviceTimeoutChecker: marked {} WAITING record(s) as FAILED (timeout={}s)",
                    updated, timeoutSeconds);
        }
    }
}
