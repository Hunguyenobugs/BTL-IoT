package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.Datasensors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSensorRepository extends JpaRepository<Datasensors, Long> {

    /** Lấy bản ghi mới nhất theo sensor_id */
    Optional<Datasensors> findTopBySensorIdOrderByMeasuredAtDesc(Long sensorId);

    /**
     * Tìm kiếm có filter + paging (native query).
     * Tham số :sort = "asc" hoặc "desc" — xử lý ORDER BY trong SQL để tránh
     * Spring Data tự ghép tên Java field (measuredAt) vào ORDER clause của native query.
     */
    @Query(
        value = """
            SELECT d.* FROM datasensors d
            JOIN sensors s ON d.sensors_id = s.id
            WHERE (:sensorName IS NULL OR s.name = :sensorName)
              AND (:timeStr IS NULL OR DATE_FORMAT(d.measured_at, '%Y-%m-%d %H:%i:%s') LIKE :timeStr)
              AND (:valuePattern IS NULL OR CAST(d.value AS CHAR) LIKE :valuePattern OR CAST(ROUND(d.value, 1) AS CHAR) LIKE :valuePattern)
            ORDER BY
              CASE WHEN :sort = 'asc'  THEN d.measured_at END ASC,
              CASE WHEN :sort = 'asc'  THEN d.id END ASC,
              CASE WHEN :sort = 'desc' THEN d.measured_at END DESC,
              CASE WHEN :sort = 'desc' THEN d.id END DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM datasensors d
            JOIN sensors s ON d.sensors_id = s.id
            WHERE (:sensorName IS NULL OR s.name = :sensorName)
              AND (:timeStr IS NULL OR DATE_FORMAT(d.measured_at, '%Y-%m-%d %H:%i:%s') LIKE :timeStr)
              AND (:valuePattern IS NULL OR CAST(d.value AS CHAR) LIKE :valuePattern OR CAST(ROUND(d.value, 1) AS CHAR) LIKE :valuePattern)
            """,
        nativeQuery = true
    )
    Page<Datasensors> findWithFilters(
            @Param("sensorName")   String sensorName,
            @Param("timeStr")      String timeStr,
            @Param("valuePattern") String valuePattern,
            @Param("sort")         String sort,
            Pageable pageable);

    /** Lấy N bản ghi gần nhất theo sensor (ORDER BY DESC trong query, service reverse để chart) */
    @Query("SELECT d FROM Datasensors d WHERE d.sensor.id = :sensorId ORDER BY d.measuredAt DESC")
    List<Datasensors> findLatestBySensorId(@Param("sensorId") Long sensorId, Pageable pageable);
}
