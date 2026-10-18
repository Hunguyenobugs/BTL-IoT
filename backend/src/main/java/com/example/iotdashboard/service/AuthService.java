package com.example.iotdashboard.service;

import com.example.iotdashboard.dto.AuthResponse;
import com.example.iotdashboard.dto.ChangePasswordRequest;
import com.example.iotdashboard.dto.LoginRequest;
import com.example.iotdashboard.dto.RegisterRequest;
import com.example.iotdashboard.entity.User;
import com.example.iotdashboard.repository.UserRepository;
import com.example.iotdashboard.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }

        User user = userRepository.findByUsername(req.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());
        log.info("User {} logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .studentCode(user.getStudentCode())
                .classField(user.getClassField())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }
        if (req.getStudentCode() == null || req.getStudentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống");
        }

        String username = req.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại trên hệ thống");
        }

        User user = new User();
        user.setUsername(username);
        // Mã hóa mật khẩu bằng BCrypt
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName().trim());
        user.setStudentCode(req.getStudentCode().trim());
        user.setClassField(req.getClassField() != null ? req.getClassField().trim() : null);
        user.setLinkFigma(req.getLinkFigma() != null ? req.getLinkFigma().trim() : null);
        user.setLinkPostman(req.getLinkPostman() != null ? req.getLinkPostman().trim() : null);
        user.setLinkGithub(req.getLinkGithub() != null ? req.getLinkGithub().trim() : null);
        user.setLinkReport(req.getLinkReport() != null ? req.getLinkReport().trim() : null);

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser.getUsername());
        log.info("User {} registered successfully", savedUser.getUsername());

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .name(savedUser.getName())
                .studentCode(savedUser.getStudentCode())
                .classField(savedUser.getClassField())
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Không xác định được danh tính người dùng");
        }
        if (req.getOldPassword() == null || req.getOldPassword().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        // Mã hóa mật khẩu mới bằng BCrypt
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user {}", username);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
    }
}
