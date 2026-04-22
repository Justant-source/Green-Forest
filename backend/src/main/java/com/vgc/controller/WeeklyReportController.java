package com.vgc.controller;

import com.vgc.dto.WeeklyReportDto;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.WeeklyReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/weekly-reports")
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;
    private final UserRepository userRepository;

    public WeeklyReportController(WeeklyReportService weeklyReportService,
                                   UserRepository userRepository) {
        this.weeklyReportService = weeklyReportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyLatestReport(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        WeeklyReportDto report = weeklyReportService.getLatestForUser(user.getId());
        if (report == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report);
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateReports(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("관리자 권한이 필요합니다.");
        }

        weeklyReportService.generateWeeklyReports(LocalDate.now());
        return ResponseEntity.ok("주간 리포트 생성 완료");
    }
}
