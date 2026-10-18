package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.Sensors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensors, Long> {
}
