package com.example.iotdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "`user`")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(length = 255, nullable = false)
    private String password;

    @Column(name = "student_code", length = 20)
    private String studentCode;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "`class`", length = 50)
    private String classField;

    /** URL hoặc base64 avatar của user */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "link_figma", length = 255)
    private String linkFigma;

    @Column(name = "link_postman", length = 255)
    private String linkPostman;

    @Column(name = "link_github", length = 255)
    private String linkGithub;

    @Column(name = "link_report", length = 255)
    private String linkReport;
}