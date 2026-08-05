package com.vgc.controller;

import com.vgc.dto.AuthRequest;
import com.vgc.dto.AuthResponse;
import com.vgc.service.ActivityLogService;
import com.vgc.service.AuthService;
import com.vgc.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ActivityLogService activityLogService;
    private final SystemSettingService systemSettingService;

    public AuthController(AuthService authService, ActivityLogService activityLogService,
                          SystemSettingService systemSettingService) {
        this.authService = authService;
        this.activityLogService = activityLogService;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/registration-open")
    public Map<String, Boolean> registrationOpen() {
        return Map.of("open", systemSettingService.isRegistrationOpen());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(
                    request.getEmail(), request.getPassword(), request.getNickname(), request.getName(),
                    request.getBirthMonth(), request.getBirthDay());
            activityLogService.logRegister(request.getEmail(), request.getNickname());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        try {
            AuthResponse response = authService.login(request.getEmail(), request.getPassword());
            activityLogService.logLogin(request.getEmail(), ip);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            activityLogService.logLoginFailed(request.getEmail(), ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호를 확인해주세요."));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
