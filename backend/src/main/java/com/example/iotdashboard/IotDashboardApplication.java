package com.example.iotdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IotDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotDashboardApplication.class, args);
    }
}
