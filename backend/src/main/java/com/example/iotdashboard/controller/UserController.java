package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.ApiResponse;
import com.example.iotdashboard.entity.User;
import com.example.iotdashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** GET /api/user — trả về thông tin sinh viên đầu tiên trong bảng */
    @GetMapping
    public ApiResponse<?> getUser() {
        Optional<User> user = userRepository.findAll().stream().findFirst();
        return user.<ApiResponse<?>>map(ApiResponse::ok)
                .orElse(ApiResponse.error("No user found"));
    }
}
