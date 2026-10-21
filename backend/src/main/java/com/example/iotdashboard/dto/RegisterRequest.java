package com.example.iotdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String name;
    private String studentCode;
    private String classField;
    private String avatarUrl;     
    private String linkFigma;
    private String linkPostman;
    private String linkGithub;
    private String linkReport;
}