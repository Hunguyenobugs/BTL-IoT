package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.Devices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Devices, Long> {
    List<Devices> findByCurrentStatus(String currentStatus);
}
