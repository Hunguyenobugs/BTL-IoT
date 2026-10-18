package com.example.iotdashboard;

import com.example.iotdashboard.security.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {

    @Test
    void testBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "123456";
        String hash = encoder.encode(raw);
        Assertions.assertTrue(encoder.matches(raw, hash));
        Assertions.assertFalse(encoder.matches("wrong_pass", hash));
    }

    @Test
    void testJwt() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "0123456789012345678901234567890123456789012345678901234567890123",
                3600000
        );
        String token = provider.generateToken("testuser");
        Assertions.assertTrue(provider.validateToken(token));
        Assertions.assertEquals("testuser", provider.getUsernameFromToken(token));
        Assertions.assertFalse(provider.validateToken("invalid.token.here"));
    }
}
