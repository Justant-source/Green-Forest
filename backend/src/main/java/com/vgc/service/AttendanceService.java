package com.vgc.service;

import com.vgc.entity.*;
import com.vgc.repository.AttendanceCheckinRepository;
import com.vgc.repository.AttendancePhraseRepository;
import com.vgc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.attendance.enforce-time-window:true}")
    private boolean enforceTimeWindow;

    private final AttendanceCheckinRepository checkinRepository;
    private final AttendancePhraseRepository phraseRepository;
    private final NotificationService notificationService;
    private final DropService dropService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final OutboundEventService outboundEventService;

    public AttendanceService(AttendanceCheckinRepository checkinRepository,
                             AttendancePhraseRepository phraseRepository,
                             NotificationService notificationService,
                             DropService dropService,
                             UserRepository userRepository,
                             ActivityLogService activityLogService,
                             OutboundEventService outboundEventService) {
        this.checkinRepository = checkinRepository;
        this.phraseRepository = phraseRepository;
        this.notificationService = notificationService;
        this.dropService = dropService;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.outboundEventService = outboundEventService;
    }

    @Transactional
    public Map<String, Object> checkin(User user, String customMessage, Long phraseId) {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dow = today.getDayOfWeek();

        if (enforceTimeWindow) {
            // 주말 거절
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                activityLogService.logAttendanceDenied(user.getId(), user.getNickname(), "주말");
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "주말은 출석하지 않아요");
            }
            // 시간 범위 검증 (06:00 <= now < 11:00)
            if (currentTime.isBefore(LocalTime.of(6, 0)) || !currentTime.isBefore(LocalTime.of(11, 0))) {
                activityLogService.logAttendanceDenied(user.getId(), user.getNickname(), "시간외(" + currentTime + ")");
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "출석 가능 시간은 평일 오전 6시~11시입니다");
            }
        }
        // message, phraseId 둘 다 없으면 400
        if ((customMessage == null || customMessage.isBlank()) && phraseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한마디를 입력하거나 추천 문구를 선택하세요");
        }
        // 중복 출석 체크
        if (checkinRepository.findByUserIdAndCheckinDate(user.getId(), today).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘 이미 출석하셨습니다");
        }

        String plantType = user.getPlantType() != null ? user.getPlantType().name() : "DEFAULT";
        String jobClass = user.getJobClass() != null ? user.getJobClass().name() : "DEFAULT";
        String stampStyle = plantType + "_" + jobClass;

        AttendanceCheckin checkin = new AttendanceCheckin();
        checkin.setUser(user);
        checkin.setCheckinDate(today);
        checkin.setCheckinAt(now.toLocalDateTime());
        checkin.setStampStyle(stampStyle);

        if (customMessage != null && !customMessage.isBlank()) {
            checkin.setMessage(customMessage.trim());
            checkin.setMessageType(AttendanceMessageType.CUSTOM);
        } else {
            AttendancePhrase phrase = phraseRepository.findById(phraseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 문구입니다"));
            checkin.setMessage(phrase.getPhrase());
            checkin.setMessageType(AttendanceMessageType.SUGGESTED);
            checkin.setSuggestedPhraseId(phraseId);
        }

        checkinRepository.save(checkin);
        dropService.awardAttendance(user);
        activityLogService.logAttendance(user.getId(), user.getNickname(), 10);

        long todayCount = checkinRepository.countByCheckinDate(today);
        long monthCount = checkinRepository.countByUserIdAndCheckinDateBetween(
                user.getId(), today.withDayOfMonth(1), today);
        int streak = calculateStreak(user.getId(), today);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkinId", checkin.getId());
        result.put("checkinAt", checkin.getCheckinAt().toString());
        result.put("stampStyle", stampStyle);
        result.put("message", checkin.getMessage());
        result.put("dropsAwarded", 10);
        result.put("todayCheckinCount", todayCount);
        result.put("monthCheckinCount", monthCount);
        result.put("streak", streak);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTodayBoard() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        boolean isDrawDone = now.toLocalTime().isAfter(LocalTime.of(11, 0)) || now.toLocalTime().equals(LocalTime.of(11, 0));

        List<AttendanceCheckin> checkins = checkinRepository.findByCheckinDateOrderByCheckinAtAsc(today);
        List<Map<String, Object>> list = checkins.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", c.getUser().getId());
            m.put("nickname", c.getUser().getNickname());
            m.put("plantType", c.getUser().getPlantType() != null ? c.getUser().getPlantType().name() : null);
            m.put("jobClass", c.getUser().getJobClass() != null ? c.getUser().getJobClass().name() : null);
            m.put("stampStyle", c.getStampStyle());
            m.put("message", c.getMessage());
            m.put("checkinAt", c.getCheckinAt().toString());
            m.put("isWinner", c.isWinner());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> winner = checkins.stream()
                .filter(AttendanceCheckin::isWinner)
                .findFirst()
                .map(c -> {
                    Map<String, Object> w = new LinkedHashMap<>();
                    w.put("userId", c.getUser().getId());
                    w.put("nickname", c.getUser().getNickname());
                    w.put("checkinAt", c.getCheckinAt().toString());
                    return w;
                }).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("checkins", list);
        result.put("isDrawDone", isDrawDone);
        result.put("winner", winner);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyMonth(User user, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<AttendanceCheckin> checkins = checkinRepository
                .findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(user.getId(), from, to);

        List<Map<String, Object>> days = checkins.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", c.getCheckinDate().toString());
            m.put("isWinner", c.isWinner());
            m.put("message", c.getMessage());
            return m;
        }).collect(Collectors.toList());

        long winCount = checkins.stream().filter(AttendanceCheckin::isWinner).count();
        int streak = calculateStreak(user.getId(), LocalDate.now(KST));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month.toString());
        result.put("days", days);
        result.put("checkinCount", checkins.size());
        result.put("winCount", winCount);
        result.put("streak", streak);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRandomPhrases(int count) {
        if (count < 1) count = 1;
        if (count > 5) count = 5;

        DayOfWeek dow = LocalDate.now(KST).getDayOfWeek();
        List<String> categories = new ArrayList<>();
        categories.add("GENERAL");
        if (dow == DayOfWeek.MONDAY) categories.add("MONDAY");
        if (dow == DayOfWeek.FRIDAY) categories.add("FRIDAY");

        List<AttendancePhrase> phrases = phraseRepository.findByActiveTrueAndCategoryIn(categories);
        Collections.shuffle(phrases, secureRandom);

        return phrases.stream().limit(count).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("phrase", p.getPhrase());
            m.put("category", p.getCategory());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> drawDailyWinner(LocalDate date) {
        // 멱등성: 이미 당첨자가 있으면 skip
        if (checkinRepository.existsByCheckinDateAndWinnerTrue(date)) {
            return Map.of("skipped", true, "reason", "이미 추첨 완료");
        }
        List<AttendanceCheckin> checkins = checkinRepository.findByCheckinDateOrderByCheckinAtAsc(date);
        if (checkins.isEmpty()) {
            return Map.of("skipped", true, "reason", "출석자 없음");
        }
        int idx = secureRandom.nextInt(checkins.size());
        AttendanceCheckin winner = checkins.get(idx);
        winner.setWinner(true);
        winner.setWinnerDrawnAt(LocalDateTime.now(KST));
        winner.setDeliveryStatus(AttendanceDeliveryStatus.PENDING);
        checkinRepository.save(winner);

        // 알림 생성
        try {
            notificationService.createNotification(
                winner.getUser(),
                NotificationType.ANNOUNCEMENT,
                "🎉 출석 당첨!",
                "오늘 커피쿠폰 당첨자로 선정되었습니다. 관리자에게 문의하세요.",
                null, null
            );
        } catch (Exception ignored) {}

        // 사내망 polling 용 outbox publish (같은 트랜잭션에 join)
        // 참여자 전체 목록을 함께 실어 회사 측에서 누가/몇시에 출석했는지 알 수 있게 한다.
        List<Map<String, Object>> participants = checkins.stream().map(c -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("userId", c.getUser().getId());
            p.put("nickname", c.getUser().getNickname());
            p.put("checkinAt", c.getCheckinAt().toString());
            p.put("message", c.getMessage());
            p.put("isWinner", c.isWinner());
            return p;
        }).collect(Collectors.toList());

        Map<String, Object> attendancePayload = new LinkedHashMap<>();
        attendancePayload.put("date", date.toString());
        attendancePayload.put("winnerId", winner.getUser().getId());
        attendancePayload.put("winnerNickname", winner.getUser().getNickname());
        attendancePayload.put("winnerCheckinAt", winner.getCheckinAt().toString());
        attendancePayload.put("winnerMessage", winner.getMessage());
        attendancePayload.put("totalCheckins", checkins.size());
        attendancePayload.put("drawnAt", LocalDateTime.now(KST).toString());
        attendancePayload.put("participants", participants);
        outboundEventService.publish("ATTENDANCE_WINNER", attendancePayload);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skipped", false);
        result.put("date", date.toString());
        result.put("winnerId", winner.getUser().getId());
        result.put("winnerNickname", winner.getUser().getNickname());
        result.put("checkinId", winner.getId());
        result.put("totalCheckins", checkins.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<com.vgc.dto.AdminAttendanceDeliveryDto> listDeliveries(AttendanceDeliveryStatus status) {
        return checkinRepository.findWinnersWithUserByDeliveryStatus(status).stream()
                .map(com.vgc.dto.AdminAttendanceDeliveryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public com.vgc.dto.AdminAttendanceDeliveryDto markDelivered(Long checkinId, Long adminId, String memo) {
        AttendanceCheckin c = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "출석 기록 없음"));
        if (!c.isWinner()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "당첨자가 아닙니다");
        }
        c.setDeliveryStatus(AttendanceDeliveryStatus.DELIVERED);
        c.setDeliveredAt(LocalDateTime.now(KST));
        c.setDeliveredBy(adminId);
        c.setDeliveryMemo(memo);
        return com.vgc.dto.AdminAttendanceDeliveryDto.from(checkinRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyWins(Long userId) {
        return checkinRepository.findMyWins(userId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("date", c.getCheckinDate().toString());
            m.put("checkinAt", c.getCheckinAt().toString());
            m.put("winnerDrawnAt", c.getWinnerDrawnAt() != null ? c.getWinnerDrawnAt().toString() : null);
            m.put("message", c.getMessage());
            m.put("deliveryStatus", c.getDeliveryStatus().name());
            m.put("deliveryMemo", c.getDeliveryMemo());
            m.put("deliveredAt", c.getDeliveredAt() != null ? c.getDeliveredAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    private int calculateStreak(Long userId, LocalDate from) {
        LocalDate to = from;
        LocalDate monthStart = from.minusMonths(3).withDayOfMonth(1);
        List<AttendanceCheckin> recent = checkinRepository
                .findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, monthStart, to);

        Set<LocalDate> checkinDates = recent.stream()
                .map(AttendanceCheckin::getCheckinDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cur = from;
        while (checkinDates.contains(cur)) {
            streak++;
            cur = cur.minusDays(1);
            // 주말 건너뜀
            while (cur.getDayOfWeek() == DayOfWeek.SATURDAY || cur.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cur = cur.minusDays(1);
            }
        }
        return streak;
    }
}
