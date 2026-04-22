package com.vgc.controller;

import com.vgc.dto.AttendanceCheckinRequest;
import com.vgc.entity.AttendanceDeliveryStatus;
import com.vgc.entity.GachaDeliveryStatus;
import com.vgc.entity.User;
import com.vgc.repository.AttendanceCheckinRepository;
import com.vgc.repository.GachaDrawRepository;
import com.vgc.repository.UserRepository;
import com.vgc.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final AttendanceCheckinRepository attendanceCheckinRepository;
    private final GachaDrawRepository gachaDrawRepository;

    public AttendanceController(AttendanceService attendanceService,
                                UserRepository userRepository,
                                AttendanceCheckinRepository attendanceCheckinRepository,
                                GachaDrawRepository gachaDrawRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        this.attendanceCheckinRepository = attendanceCheckinRepository;
        this.gachaDrawRepository = gachaDrawRepository;
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

    @GetMapping("/me/wins")
    public ResponseEntity<List<Map<String, Object>>> getMyWins(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(attendanceService.getMyWins(user.getId()));
    }

    // 헤더 "당첨" 배지 — 미수령 보상 합계 (출석 + 가챠). total=0 이면 배지 숨김.
    @GetMapping("/me/pending-rewards")
    public ResponseEntity<Map<String, Object>> getMyPendingRewards(Authentication authentication) {
        User user = getUser(authentication);
        long att = attendanceCheckinRepository.countByUserIdAndWinnerTrueAndDeliveryStatus(
                user.getId(), AttendanceDeliveryStatus.PENDING);
        long gacha = gachaDrawRepository.countByUserIdAndWinnerTrueAndDeliveryStatus(
                user.getId(), GachaDeliveryStatus.PENDING);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("attendance", att);
        r.put("gacha", gacha);
        r.put("total", att + gacha);
        return ResponseEntity.ok(r);
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
