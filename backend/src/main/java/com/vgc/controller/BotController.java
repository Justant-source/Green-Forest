package com.vgc.controller;

import com.vgc.entity.AttendanceCheckin;
import com.vgc.entity.User;
import com.vgc.repository.AttendanceCheckinRepository;
import com.vgc.repository.UserRepository;
import com.vgc.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final AttendanceCheckinRepository checkinRepository;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public BotController(AttendanceCheckinRepository checkinRepository,
                         AttendanceService attendanceService,
                         UserRepository userRepository) {
        this.checkinRepository = checkinRepository;
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/attendance/winner")
    public ResponseEntity<Map<String, Object>> getWinner(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        List<AttendanceCheckin> checkins = checkinRepository.findByCheckinDateOrderByCheckinAtAsc(d);
        long totalCheckins = checkinRepository.countByCheckinDate(d);

        Optional<AttendanceCheckin> winner = checkins.stream().filter(AttendanceCheckin::isWinner).findFirst();
        if (winner.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("date", date);
            result.put("drawDone", false);
            result.put("winner", null);
            result.put("totalCheckins", totalCheckins);
            return ResponseEntity.ok(result);
        }

        AttendanceCheckin w = winner.get();
        Map<String, Object> winnerInfo = new LinkedHashMap<>();
        winnerInfo.put("userId", w.getUser().getId());
        winnerInfo.put("nickname", w.getUser().getNickname());
        winnerInfo.put("email", w.getUser().getEmail());
        winnerInfo.put("plantName", w.getUser().getPlantName());
        winnerInfo.put("checkinAt", w.getCheckinAt().toString());
        winnerInfo.put("message", w.getMessage());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("drawDone", true);
        result.put("winner", winnerInfo);
        result.put("totalCheckins", totalCheckins);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attendance/today-summary")
    public ResponseEntity<Map<String, Object>> todaySummary() {
        return ResponseEntity.ok(attendanceService.getTodayBoard());
    }

    @GetMapping("/attendance/broadcast-checkin-reminder")
    public ResponseEntity<List<Map<String, Object>>> broadcastReminder() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        List<AttendanceCheckin> checkins = checkinRepository.findByCheckinDateOrderByCheckinAtAsc(today);
        Set<Long> checkedInIds = checkins.stream().map(c -> c.getUser().getId()).collect(Collectors.toSet());
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> notCheckedIn = allUsers.stream()
                .filter(u -> !checkedInIds.contains(u.getId()) && "USER".equals(u.getRole()))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", u.getId());
                    m.put("nickname", u.getNickname());
                    m.put("email", u.getEmail());
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(notCheckedIn);
    }
}
