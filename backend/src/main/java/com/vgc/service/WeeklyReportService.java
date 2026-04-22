package com.vgc.service;

import com.vgc.dto.WeeklyReportDto;
import com.vgc.entity.Party;
import com.vgc.entity.User;
import com.vgc.entity.WeeklyReport;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.UserRepository;
import com.vgc.repository.WeeklyReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final DropTransactionRepository dropTransactionRepository;

    public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
                                UserRepository userRepository,
                                DropTransactionRepository dropTransactionRepository) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.dropTransactionRepository = dropTransactionRepository;
    }

    @Transactional
    public void generateWeeklyReports(LocalDate anchorDate) {
        // anchorDate 기준 이전 주(월~일) 계산
        LocalDate weekStart = getWeekStart(anchorDate);
        LocalDate weekEnd = weekStart.plus(6, ChronoUnit.DAYS);

        // 해당 주차가 이미 생성됐는지 확인
        List<User> allUsers = userRepository.findAll();
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.plus(1, ChronoUnit.DAYS).atStartOfDay();

        for (User user : allUsers) {
            if (weeklyReportRepository.existsByUserIdAndWeekStart(user.getId(), weekStart)) {
                continue; // 이미 있으면 스킵
            }

            // 해당 주간 양수 earnedAmount 합계 (sumPositiveAmountGroupByUserForPeriod 사용)
            List<Object[]> results = dropTransactionRepository
                    .sumPositiveAmountGroupByUserForPeriod(startDateTime, endDateTime);
            int positiveEarnedAmount = 0;
            for (Object[] row : results) {
                if (((Number) row[0]).longValue() == user.getId()) {
                    positiveEarnedAmount = ((Number) row[1]).intValue();
                    break;
                }
            }

            // 파티 내 랭크 계산
            Integer partyRank = null;
            if (user.getParty() != null) {
                partyRank = calculatePartyRank(user, startDateTime, endDateTime);
            }

            // WeeklyReport 저장
            WeeklyReport report = new WeeklyReport();
            report.setUser(user);
            report.setWeekStart(weekStart);
            report.setWeekEnd(weekEnd);
            report.setEarnedAmount(positiveEarnedAmount);
            report.setPartyRank(partyRank);
            weeklyReportRepository.save(report);
        }
    }

    private LocalDate getWeekStart(LocalDate date) {
        // 월요일 기준 주차 시작 (ISO 주간)
        // 월요일(1) ~ 일요일(7)
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=월, 7=일
        return date.minus(dayOfWeek - 1, ChronoUnit.DAYS);
    }

    private Integer calculatePartyRank(User user, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (user.getParty() == null) {
            return null;
        }

        Party party = user.getParty();
        List<User> partyMembers = userRepository.findByPartyIdOrderByTotalDropsDesc(party.getId());

        // 해당 주간 각 멤버의 earnedAmount 계산
        Map<Long, Integer> memberEarnings = new HashMap<>();
        List<Object[]> results = dropTransactionRepository
                .sumPositiveAmountGroupByUserForPeriod(startDateTime, endDateTime);
        for (Object[] row : results) {
            memberEarnings.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        // 같은 파티 멤버들의 earnedAmount desc 정렬
        List<User> sortedMembers = partyMembers.stream()
                .sorted((u1, u2) -> {
                    int earn1 = memberEarnings.getOrDefault(u1.getId(), 0);
                    int earn2 = memberEarnings.getOrDefault(u2.getId(), 0);
                    return Integer.compare(earn2, earn1); // 내림차순
                })
                .collect(Collectors.toList());

        // 유저의 rank 찾기
        for (int i = 0; i < sortedMembers.size(); i++) {
            if (sortedMembers.get(i).getId().equals(user.getId())) {
                return i + 1; // 1부터 시작
            }
        }

        return null;
    }

    public WeeklyReportDto getLatestForUser(Long userId) {
        return weeklyReportRepository.findTopByUserIdOrderByWeekStartDesc(userId)
                .map(WeeklyReportDto::from)
                .orElse(null);
    }
}
