package com.vgc.service;

import com.vgc.entity.User;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final UserRepository userRepository;
    private final DropTransactionRepository dropTransactionRepository;

    public LeaderboardService(UserRepository userRepository,
                              DropTransactionRepository dropTransactionRepository) {
        this.userRepository = userRepository;
        this.dropTransactionRepository = dropTransactionRepository;
    }

    /**
     * 파티별 랭킹 (메인 뷰)
     * @param period "monthly" | "all_time"
     */
    public List<Map<String, Object>> getPartyRankings(String period) {
        if ("monthly".equals(period)) {
            return getMonthlyPartyRankings();
        }
        return getAllTimePartyRankings();
    }

    /**
     * 전체 기간 파티별 랭킹 — users.total_drops 합산
     */
    private List<Map<String, Object>> getAllTimePartyRankings() {
        List<Object[]> rows = userRepository.getPartyRankings();
        List<Map<String, Object>> rankings = new ArrayList<>();

        int rank = 1;
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("partyId", row[0]);
            entry.put("partyName", row[1]);
            entry.put("totalDrops", ((Number) row[2]).longValue());
            entry.put("memberCount", ((Number) row[3]).longValue());
            rankings.add(entry);
        }
        return rankings;
    }

    /**
     * 이번 달 파티별 랭킹 — drop_transactions 이번 달 합산
     */
    private List<Map<String, Object>> getMonthlyPartyRankings() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 이번 달 유저별 합산
        List<Object[]> userSums = dropTransactionRepository.sumAmountGroupByUserForPeriod(startDate, endDate);
        Map<Long, Long> userDropMap = new HashMap<>();
        for (Object[] row : userSums) {
            userDropMap.put((Long) row[0], ((Number) row[1]).longValue());
        }

        // 파티별로 그룹핑
        List<User> allUsers = userRepository.findAllWithPartyOrderByTotalDropsDesc();
        Map<Long, List<User>> partyUsers = allUsers.stream()
                .filter(u -> u.getParty() != null)
                .collect(Collectors.groupingBy(u -> u.getParty().getId()));

        List<Map<String, Object>> rankings = new ArrayList<>();
        for (Map.Entry<Long, List<User>> entry : partyUsers.entrySet()) {
            List<User> members = entry.getValue();
            long partyTotal = members.stream()
                    .mapToLong(u -> userDropMap.getOrDefault(u.getId(), 0L))
                    .sum();

            Map<String, Object> partyEntry = new LinkedHashMap<>();
            partyEntry.put("partyId", entry.getKey());
            partyEntry.put("partyName", members.get(0).getParty().getName());
            partyEntry.put("totalDrops", partyTotal);
            partyEntry.put("memberCount", (long) members.size());
            rankings.add(partyEntry);
        }

        rankings.sort((a, b) -> Long.compare((Long) b.get("totalDrops"), (Long) a.get("totalDrops")));

        int rank = 1;
        for (Map<String, Object> entry : rankings) {
            entry.put("rank", rank++);
        }

        return rankings;
    }

    /**
     * 파티 내 개인별 랭킹
     */
    public List<Map<String, Object>> getPartyMemberRankings(Long partyId, String period) {
        List<User> members = userRepository.findByPartyIdOrderByTotalDropsDesc(partyId);

        Map<Long, Long> monthlyDrops = null;
        if ("monthly".equals(period)) {
            YearMonth currentMonth = YearMonth.now();
            LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime endDate = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

            List<Object[]> sums = dropTransactionRepository.sumAmountGroupByUserForPeriod(startDate, endDate);
            monthlyDrops = new HashMap<>();
            for (Object[] row : sums) {
                monthlyDrops.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        List<Map<String, Object>> rankings = new ArrayList<>();
        int rank = 1;

        Map<Long, Long> finalMonthlyDrops = monthlyDrops;
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (User member : members) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId", member.getId());
            entry.put("nickname", member.getNickname());
            entry.put("jobClass", member.getJobClass() != null ? member.getJobClass().name() : null);
            entry.put("jobClassLabel", member.getJobClass() != null ? member.getJobClass().getLabel() : null);
            entry.put("plantType", member.getPlantType() != null ? member.getPlantType().name() : null);

            if ("monthly".equals(period) && finalMonthlyDrops != null) {
                entry.put("totalDrops", finalMonthlyDrops.getOrDefault(member.getId(), 0L));
            } else {
                entry.put("totalDrops", (long) member.getTotalDrops());
            }
            memberList.add(entry);
        }

        // 정렬 후 순위 부여
        memberList.sort((a, b) -> Long.compare((Long) b.get("totalDrops"), (Long) a.get("totalDrops")));
        for (Map<String, Object> entry : memberList) {
            entry.put("rank", rank++);
        }

        return memberList;
    }
}
