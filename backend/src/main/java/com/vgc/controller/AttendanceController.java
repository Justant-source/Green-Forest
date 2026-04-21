package com.vgc.controller;

import com.vgc.dto.AttendanceCheckinRequest;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public AttendanceController(AttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> checkin(
            Authentication authentication,
            @RequestBody AttendanceCheckinRequest req) {
        User user = getUser(authentication);
        Map<String, Object> result = attendanceService.checkin(user, req.getMessage(), req.getPhraseId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayBoard() {
        return ResponseEntity.ok(attendanceService.getTodayBoard());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyMonth(
            Authentication authentication,
            @RequestParam(defaultValue = "") String month) {
        User user = getUser(authentication);
        YearMonth ym = month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        return ResponseEntity.ok(attendanceService.getMyMonth(user, ym));
    }

    @GetMapping("/phrases/random")
    public ResponseEntity<List<Map<String, Object>>> getRandomPhrases(
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(attendanceService.getRandomPhrases(count));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
