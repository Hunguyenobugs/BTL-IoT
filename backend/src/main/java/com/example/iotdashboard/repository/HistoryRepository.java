package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.History;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    /** Tìm bản ghi WAITING mới nhất theo device_id */
    Optional<History> findTopByDeviceIdAndStatusOrderByIdDesc(Long deviceId, String status);

    /** Tìm danh sách bản ghi WAITING quá hạn */
    List<History> findByStatusAndExecutedAtBefore(String status, LocalDateTime cutoff);

    /** Kiểm tra xem thiết bị còn bản ghi theo status không */
    boolean existsByDeviceIdAndStatus(Long deviceId, String status);

    /** Bulk update tất cả WAITING quá hạn thành FAILED */
    @Modifying
    @Query("""
        UPDATE History h
        SET h.status = 'FAILED', h.executedAt = :now
        WHERE h.status = 'WAITING' AND h.executedAt < :cutoff
        """)
    int markTimedOutAsFailed(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);

    /**
     * Tìm kiếm lịch sử với filter + paging (native query).
     * Tham số :sort = "asc" hoặc "desc" — xử lý ORDER BY trong SQL để tránh
     * Spring Data tự ghép tên Java field (executedAt) vào ORDER clause của native query.
     */
    @Query(
        value = """
            SELECT h.* FROM history h
            JOIN devices d ON h.device_id = d.id
            WHERE (:deviceName IS NULL OR d.name = :deviceName)
              AND (:timeStr IS NULL OR DATE_FORMAT(h.executed_at, '%Y-%m-%d %H:%i:%s') LIKE :timeStr)
              AND (:action IS NULL OR h.action = :action)
              AND (:status IS NULL OR h.status = :status OR (:status = 'SUCCESS' AND h.status IN ('ON', 'OFF')))
            ORDER BY
              CASE WHEN :sort = 'asc'  THEN h.executed_at END ASC,
              CASE WHEN :sort = 'desc' THEN h.executed_at END DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM history h
            JOIN devices d ON h.device_id = d.id
            WHERE (:deviceName IS NULL OR d.name = :deviceName)
              AND (:timeStr IS NULL OR DATE_FORMAT(h.executed_at, '%Y-%m-%d %H:%i:%s') LIKE :timeStr)
              AND (:action IS NULL OR h.action = :action)
              AND (:status IS NULL OR h.status = :status OR (:status = 'SUCCESS' AND h.status IN ('ON', 'OFF')))
            """,
        nativeQuery = true
    )
    Page<History> findWithFilters(
            @Param("deviceName") String deviceName,
            @Param("timeStr")    String timeStr,
            @Param("action")     String action,
            @Param("status")     String status,
            @Param("sort")       String sort,
            Pageable pageable);
}
