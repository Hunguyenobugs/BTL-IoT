package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.ApiResponse;
import com.example.iotdashboard.dto.AuthResponse;
import com.example.iotdashboard.dto.ChangePasswordRequest;
import com.example.iotdashboard.dto.LoginRequest;
import com.example.iotdashboard.dto.RegisterRequest;
import com.example.iotdashboard.entity.User;
import com.example.iotdashboard.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(Authentication authentication,
                                              @RequestBody ChangePasswordRequest request) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Yêu cầu đăng nhập để đổi mật khẩu");
        }
        authService.changePassword(authentication.getName(), request);
        return ApiResponse.ok("Đổi mật khẩu thành công");
    }

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }
        User user = authService.getCurrentUser(authentication.getName());
        return ApiResponse.ok(user);
    }
}
