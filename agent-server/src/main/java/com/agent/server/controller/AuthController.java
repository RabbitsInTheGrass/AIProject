package com.agent.server.controller;

import com.agent.server.model.dto.AuthResponse;
import com.agent.server.model.dto.LoginRequest;
import com.agent.server.model.dto.RegisterRequest;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
