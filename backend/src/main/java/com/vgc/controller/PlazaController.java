package com.vgc.controller;

import com.vgc.entity.AttendanceCheckin;
import com.vgc.entity.GachaDraw;
import com.vgc.repository.AttendanceCheckinRepository;
import com.vgc.repository.GachaDrawRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plaza")
public class PlazaController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GachaDrawRepository gachaDrawRepository;
    private final AttendanceCheckinRepository attendanceCheckinRepository;

    public PlazaController(GachaDrawRepository gachaDrawRepository,
                           AttendanceCheckinRepository attendanceCheckinRepository) {
        this.gachaDrawRepository = gachaDrawRepository;
        this.attendanceCheckinRepository = attendanceCheckinRepository;
    }

    @GetMapping("/winners")
    public List<Map<String, Object>> getWeeklyWinners() {
        LocalDate today = LocalDate.now(KST);
        LocalDate weekAgo = today.minusDays(7);
        LocalDateTime weekAgoDateTime = weekAgo.atStartOfDay();

        List<Map<String, Object>> result = new ArrayList<>();

        // 뽑기 당첨 (최근 7일)
        List<GachaDraw> gachaWins = gachaDrawRepository
                .findByWinnerTrueAndCreatedAtAfterOrderByCreatedAtDesc(weekAgoDateTime);
        for (GachaDraw draw : gachaWins) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "GACHA");
            item.put("userNickname", draw.getUser().getNickname());
            item.put("prizeName", draw.getPrizeName());
            item.put("createdAt", draw.getCreatedAt().toString());
            result.add(item);
        }

        // 출석 당첨 (최근 7일)
        List<AttendanceCheckin> attendanceWins = attendanceCheckinRepository
                .findWinnersBetween(weekAgo, today);
        for (AttendanceCheckin checkin : attendanceWins) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "ATTENDANCE");
            item.put("userNickname", checkin.getUser().getNickname());
            item.put("checkinDate", checkin.getCheckinDate().toString());
            // 출석 당첨은 추첨 시각(보통 당일 11:00) 기준으로 표시
            LocalDateTime drawAt = checkin.getWinnerDrawnAt() != null
                    ? checkin.getWinnerDrawnAt()
                    : checkin.getCheckinDate().atTime(11, 0);
            item.put("createdAt", drawAt.toString());
            result.add(item);
        }

        // 최신순 정렬
        result.sort(Comparator.comparing(m -> (String) m.get("createdAt"), Comparator.reverseOrder()));

        return result;
    }
}
