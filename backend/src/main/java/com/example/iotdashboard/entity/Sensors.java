package com.example.iotdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sensors")
@Getter @Setter @NoArgsConstructor
public class Sensors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ví dụ: "Nhiệt độ", "Độ ẩm", "Ánh sáng" */
    @Column(length = 50, nullable = false)
    private String name;

    /** Ví dụ: "°C", "%", "lux" */
    @Column(length = 10, nullable = false)
    private String unit;
}
