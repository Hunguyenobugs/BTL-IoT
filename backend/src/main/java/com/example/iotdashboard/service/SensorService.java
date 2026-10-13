package com.example.iotdashboard.service;

import com.example.iotdashboard.dto.*;
import com.example.iotdashboard.entity.Datasensors;
import com.example.iotdashboard.entity.Sensors;
import com.example.iotdashboard.repository.DataSensorRepository;
import com.example.iotdashboard.repository.SensorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorService {

    private final SensorRepository sensorRepository;
    private final DataSensorRepository dataSensorRepository;

    /**
     * Cache: tên cảm biến → entity Sensors.
     * Được tải lúc startup, tránh query lặp lại mỗi lần nhận MQTT message.
     * Key: payload field name (temperature/humidity/light) → Sensors entity
     */
    private final Map<String, Sensors> sensorCache = new HashMap<>();

    /** Tên cảm biến trong DB → key payload MQTT */
    private static final Map<String, String> DB_NAME_TO_PAYLOAD_KEY = Map.of(
            "Nhiệt độ", "temperature",
            "Độ ẩm",   "humidity",
            "Ánh sáng", "light"
    );

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Load sensor map sau khi toàn bộ ứng dụng (kể cả SQL init scripts) đã sẵn sàng.
     * Dùng ApplicationReadyEvent thay vì @PostConstruct để tránh race condition với data.sql.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSensorCache() {
        List<Sensors> all = sensorRepository.findAll();
        for (Sensors s : all) {
            String payloadKey = DB_NAME_TO_PAYLOAD_KEY.get(s.getName());
            if (payloadKey != null) {
                sensorCache.put(payloadKey, s);
            }
        }
        log.info("Sensor cache loaded: {}", sensorCache.keySet());
    }

    /**
     * Nhận dữ liệu từ MQTT topic data/sensors, lưu 3 bản ghi vào Datasensors.
     * Payload: { "temperature": 28.5, "humidity": 65, "light": 420, "timestamp": "..." }
     */
    @Transactional
    public void saveSensorData(JsonNode json) {
        // Luôn dùng thời gian server (ESP32 không gửi timestamp nữa)
        LocalDateTime measuredAt = LocalDateTime.now();

        String[] fields = {"temperature", "humidity", "light"};
        boolean cacheReloaded = false;  // chỉ reload tối đa 1 lần mỗi message

        for (String field : fields) {
            JsonNode node = json.path(field);
            if (node.isMissingNode() || node.isNull()) continue;

            Sensors sensor = sensorCache.get(field);

            // Nếu cache miss và chưa thử reload → tự reload từ DB rồi thử lại
            if (sensor == null && !cacheReloaded) {
                log.warn("Cache miss for '{}', reloading sensor cache from DB...", field);
                initSensorCache();
                cacheReloaded = true;
                sensor = sensorCache.get(field);
            }

            if (sensor == null) {
                log.warn("Sensor '{}' not found in DB. Ensure data.sql seeded sensors table.", field);
                continue;
            }

            Datasensors record = new Datasensors();
            record.setSensor(sensor);
            record.setValue(node.asDouble());
            record.setMeasuredAt(measuredAt);
            dataSensorRepository.save(record);
        }
    }

    /** GET /api/sensors/latest — giá trị mới nhất từng loại */
    @Transactional(readOnly = true)
    public SensorLatestResponse getLatest() {
        SensorLatestResponse resp = new SensorLatestResponse();

        for (Map.Entry<String, Sensors> e : sensorCache.entrySet()) {
            String key = e.getKey();
            Sensors sensor = e.getValue();
            dataSensorRepository.findTopBySensorIdOrderByMeasuredAtDesc(sensor.getId())
                    .ifPresent(d -> {
                        var dto = new SensorLatestResponse.SensorValueDto(
                                d.getValue(),
                                sensor.getUnit(),
                                d.getMeasuredAt().format(ISO_FMT)
                        );
                        switch (key) {
                            case "temperature" -> resp.setTemperature(dto);
                            case "humidity"    -> resp.setHumidity(dto);
                            case "light"       -> resp.setLight(dto);
                        }
                    });
        }
        return resp;
    }

    /** GET /api/sensors/chart?limit=15 — dữ liệu vẽ biểu đồ */
    @Transactional(readOnly = true)
    public SensorChartResponse getChartData(int limit) {
        // Lấy N bản ghi mới nhất mỗi loại (JPQL đã ORDER BY DESC)
        // Sau đó reverse để vẽ chart theo thứ tự thời gian (cũ → mới)
        Pageable topN = PageRequest.of(0, limit);

        Sensors tempSensor  = sensorCache.get("temperature");
        Sensors humSensor   = sensorCache.get("humidity");
        Sensors lightSensor = sensorCache.get("light");

        List<Datasensors> tempRows  = tempSensor  != null ? dataSensorRepository.findLatestBySensorId(tempSensor.getId(),  topN) : List.of();
        List<Datasensors> humRows   = humSensor   != null ? dataSensorRepository.findLatestBySensorId(humSensor.getId(),   topN) : List.of();
        List<Datasensors> lightRows = lightSensor != null ? dataSensorRepository.findLatestBySensorId(lightSensor.getId(), topN) : List.of();

        // Reverse để được thứ tự cũ → mới
        Collections.reverse(tempRows);
        Collections.reverse(humRows);
        Collections.reverse(lightRows);

        // Lấy labels từ mảng dài nhất
        List<Datasensors> longest = tempRows.size() >= humRows.size() ? tempRows : humRows;
        if (lightRows.size() > longest.size()) longest = lightRows;

        List<String> labels = longest.stream()
                .map(d -> d.getMeasuredAt().format(DT_FMT))
                .toList();

        List<Double> temps  = tempRows.stream().map(Datasensors::getValue).toList();
        List<Double> hums   = humRows.stream().map(Datasensors::getValue).toList();
        List<Double> lights = lightRows.stream().map(Datasensors::getValue).toList();

        return new SensorChartResponse(labels, temps, hums, lights);
    }

    /** GET /api/sensors — danh sách lịch sử có filter/sort/paging */
    @Transactional(readOnly = true)
    public PagedResponse<SensorDataItem> getSensorList(
            String type, String time, String value, String sort, int page, int size) {

        // Resolve sensor name filter
        String sensorName = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
            // Frontend gửi "TEMPERATURE"/"HUMIDITY"/"LIGHT" hoặc tên tiếng Việt
            sensorName = switch (type.toUpperCase().trim()) {
                case "TEMPERATURE", "NHIỆT ĐỘ" -> "Nhiệt độ";
                case "HUMIDITY",    "ĐỘ ẨM"    -> "Độ ẩm";
                case "LIGHT",       "ÁNH SÁNG"  -> "Ánh sáng";
                default -> type.trim(); // thử truyền thẳng
            };
        }

        // Value filter: tìm kiếm tương đối (LIKE %value%)
        String valuePattern = null;
        if (value != null && !value.isBlank()) {
            String cleanedVal = value.trim()
                    .replace(',', '.')
                    .replaceAll("[^0-9.-]", "");
            if (!cleanedVal.isBlank()) {
                valuePattern = "%" + cleanedVal + "%";
            }
        }

        String timeStr = (time != null && !time.isBlank()) ? time.trim() : null;
        // Bỏ Sort khỏi Pageable — native query tự xử lý ORDER BY qua param :sort
        String sortParam = "asc".equalsIgnoreCase(sort) ? "asc" : "desc";
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Datasensors> dbPage = dataSensorRepository.findWithFilters(
                sensorName, timeStr, valuePattern, sortParam, pageable);

        List<SensorDataItem> items = dbPage.getContent().stream()
                .map(d -> new SensorDataItem(
                        d.getId(),
                        d.getSensor().getName(),
                        formatValue(d.getValue(), d.getSensor().getUnit()),
                        d.getMeasuredAt().format(ISO_FMT)
                ))
                .toList();

        return new PagedResponse<>(items, page, dbPage.getTotalPages(), dbPage.getTotalElements());
    }

    private String formatValue(Double value, String unit) {
        // Nếu là số nguyên, bỏ phần thập phân
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return (long) value.doubleValue() + unit;
        }
        return String.format("%.1f", value) + unit;
    }
}
