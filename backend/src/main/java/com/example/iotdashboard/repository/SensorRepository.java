package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.Sensors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensors, Long> {

    /** Tìm loại cảm biến theo tên, dùng khi cache sensor name → id */
    Optional<Sensors> findByName(String name);
}
