package com.example.iotdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "datasensors")
@Getter @Setter @NoArgsConstructor
public class Datasensors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensors_id", nullable = false)
    private Sensors sensor;

    @Column(nullable = false)
    private Double value;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;
}
