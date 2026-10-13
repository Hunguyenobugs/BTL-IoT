package com.example.iotdashboard.repository;

import com.example.iotdashboard.entity.Devices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Devices, Long> {
    // Các thao tác CRUD cơ bản đã đủ thông qua JpaRepository
}
