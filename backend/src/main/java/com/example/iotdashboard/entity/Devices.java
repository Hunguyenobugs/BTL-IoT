package com.example.iotdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor
public class Devices {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String type;

    /** ON | OFF | WAITING — trạng thái hiện tại cache */
    @Column(name = "current_status", length = 10, nullable = false)
    private String currentStatus = "OFF";
}
