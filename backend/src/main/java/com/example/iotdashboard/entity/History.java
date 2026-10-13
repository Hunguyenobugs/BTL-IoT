package com.example.iotdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "history")
@Getter @Setter @NoArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người dùng thực hiện lệnh điều khiển */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Devices device;

    /** Hành động yêu cầu: ON | OFF */
    @Column(length = 10, nullable = false)
    private String action;

    /**
     * Trạng thái thực tế: WAITING | ON | OFF | FAILED.
     * Khi resolve: cập nhật status + ghi đè executed_at = now
     * (chỉ có 1 cột thời gian theo ERD)
     */
    @Column(length = 10, nullable = false)
    private String status = "WAITING";

    /**
     * Cột thời gian duy nhất:
     * - Khi tạo (status=WAITING): ghi nhận thời điểm request
     * - Khi resolve: ghi đè thành thời điểm kết thúc
     */
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
