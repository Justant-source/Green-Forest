package com.vgc.controller;

import com.vgc.entity.*;
import com.vgc.repository.PartyRepository;
import com.vgc.repository.UserRepository;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.PostRepository;
import com.vgc.service.CategoryService;
import com.vgc.service.DropService;
import com.vgc.service.NotificationService;
import com.vgc.service.QuestService;
import com.vgc.dto.CategoryRequestResponse;
import com.vgc.dto.CategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final DropService dropService;
    private final QuestService questService;
    private final PartyRepository partyRepository;
    private final DropTransactionRepository dropTransactionRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public AdminController(CategoryService categoryService, UserRepository userRepository,
                           DropService dropService, QuestService questService,
                           PartyRepository partyRepository,
                           DropTransactionRepository dropTransactionRepository,
                           PostRepository postRepository,
                           NotificationService notificationService) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.dropService = dropService;
        this.questService = questService;
        this.partyRepository = partyRepository;
        this.dropTransactionRepository = dropTransactionRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }

    private User getAdminUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        return user;
    }

    // ========== 카테고리 관리 (기존) ==========

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories(Authentication authentication) {
        getAdminUser(authentication);
        return categoryService.getAllCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        getAdminUser(authentication);
        String name = (String) body.get("name");
        String label = (String) body.get("label");
        String color = (String) body.get("color");
        boolean hasStatus = Boolean.TRUE.equals(body.get("hasStatus"));
        return ResponseEntity.ok(categoryService.createCategory(name, label, color, hasStatus));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/category-requests")
    public List<CategoryRequestResponse> getPendingRequests(Authentication authentication) {
        getAdminUser(authentication);
        return categoryService.getPendingRequests();
    }

    @PostMapping("/category-requests/{id}/approve")
    public ResponseEntity<CategoryResponse> approveRequest(
            @PathVariable Long id, @RequestBody Map<String, Object> body, Authentication authentication) {
        getAdminUser(authentication);
        String label = (String) body.get("label");
        String color = (String) body.get("color");
        boolean hasStatus = Boolean.TRUE.equals(body.get("hasStatus"));
        return ResponseEntity.ok(categoryService.approveRequest(id, label, color, hasStatus));
    }

    @PostMapping("/category-requests/{id}/reject")
    public ResponseEntity<CategoryRequestResponse> rejectRequest(
            @PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        getAdminUser(authentication);
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(categoryService.rejectRequest(id, reason));
    }

    // ========== 물방울 수동 지급/차감 ==========

    @PostMapping("/drops/award")
    public Map<String, String> awardDrops(@RequestBody Map<String, Object> body, Authentication authentication) {
        User admin = getAdminUser(authentication);
        Long userId = ((Number) body.get("userId")).longValue();
        int amount = ((Number) body.get("amount")).intValue();
        String reason = (String) body.get("reason");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        dropService.gmManualAward(admin, target, amount, reason);
        return Map.of("status", "awarded", "newTotal", String.valueOf(target.getTotalDrops()));
    }

    @PostMapping("/drops/deduct")
    public Map<String, String> deductDrops(@RequestBody Map<String, Object> body, Authentication authentication) {
        User admin = getAdminUser(authentication);
        Long userId = ((Number) body.get("userId")).longValue();
        int amount = ((Number) body.get("amount")).intValue();
        String reason = (String) body.get("reason");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        dropService.gmManualAward(admin, target, -Math.abs(amount), reason);
        return Map.of("status", "deducted", "newTotal", String.valueOf(target.getTotalDrops()));
    }

    // ========== 퀘스트 관리 ==========

    @PostMapping("/quests")
    public Map<String, Object> createQuest(@RequestBody Map<String, Object> body, Authentication authentication) {
        User admin = getAdminUser(authentication);
        Quest quest = questService.createQuest(
                admin,
                (String) body.get("title"),
                (String) body.get("description"),
                ((Number) body.get("rewardDrops")).intValue(),
                LocalDate.parse((String) body.get("startDate")),
                LocalDate.parse((String) body.get("endDate")),
                (String) body.getOrDefault("targetType", "전체"),
                body.get("targetPartyId") != null ? ((Number) body.get("targetPartyId")).longValue() : null,
                body.get("maxCompletionsPerUser") != null ? ((Number) body.get("maxCompletionsPerUser")).intValue() : 1,
                Boolean.TRUE.equals(body.get("isVoteType"))
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", quest.getId());
        result.put("title", quest.getTitle());
        result.put("status", "created");
        return result;
    }

    @PutMapping("/quests/{id}")
    public Map<String, String> updateQuest(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body,
                                            Authentication authentication) {
        getAdminUser(authentication);
        questService.updateQuest(id,
                (String) body.get("title"),
                (String) body.get("description"),
                ((Number) body.get("rewardDrops")).intValue(),
                LocalDate.parse((String) body.get("startDate")),
                LocalDate.parse((String) body.get("endDate")),
                (String) body.getOrDefault("targetType", "전체"),
                body.get("targetPartyId") != null ? ((Number) body.get("targetPartyId")).longValue() : null,
                body.get("maxCompletionsPerUser") != null ? ((Number) body.get("maxCompletionsPerUser")).intValue() : 1,
                body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive"))
        );
        return Map.of("status", "updated");
    }

    @DeleteMapping("/quests/{id}")
    public Map<String, String> deleteQuest(@PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        questService.deleteQuest(id);
        return Map.of("status", "deleted");
    }

    // ========== 유저 관리 ==========

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers(Authentication authentication) {
        getAdminUser(authentication);
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("nickname", u.getNickname());
            map.put("role", u.getRole());
            map.put("plantType", u.getPlantType() != null ? u.getPlantType().name() : null);
            map.put("plantName", u.getPlantName());
            map.put("jobClass", u.getJobClass() != null ? u.getJobClass().name() : null);
            map.put("partyId", u.getParty() != null ? u.getParty().getId() : null);
            map.put("partyName", u.getParty() != null ? u.getParty().getName() : null);
            map.put("totalDrops", u.getTotalDrops());
            result.add(map);
        }
        return result;
    }

    @PutMapping("/users/{id}")
    public Map<String, String> updateUser(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication authentication) {
        getAdminUser(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (body.containsKey("partyId")) {
            if (body.get("partyId") == null) {
                user.setParty(null);
            } else {
                Long partyId = ((Number) body.get("partyId")).longValue();
                Party party = partyRepository.findById(partyId)
                        .orElseThrow(() -> new RuntimeException("파티를 찾을 수 없습니다."));
                user.setParty(party);
            }
        }

        if (body.containsKey("plantType") && body.get("plantType") != null) {
            PlantType plantType = PlantType.valueOf((String) body.get("plantType"));
            user.setPlantType(plantType);
            // 식물→직업군 자동 매핑
            applyPlantJobMapping(user, plantType);
        }

        if (body.containsKey("nickname")) {
            user.setNickname((String) body.get("nickname"));
        }

        userRepository.save(user);
        return Map.of("status", "updated");
    }

    private void applyPlantJobMapping(User user, PlantType plantType) {
        switch (plantType) {
            case TABLE_PALM -> {
                user.setJobClass(JobClass.TANKER);
                user.setElement(Element.EARTH);
                user.setDifficulty(Difficulty.EASY);
                user.setExpMultiplier(Difficulty.EASY.getMultiplier());
            }
            case SPATHIPHYLLUM -> {
                user.setJobClass(JobClass.HEALER);
                user.setElement(Element.WATER);
                user.setDifficulty(Difficulty.EASY);
                user.setExpMultiplier(Difficulty.EASY.getMultiplier());
            }
            case HONG_KONG_PALM -> {
                user.setJobClass(JobClass.BUFFER);
                user.setElement(Element.WIND);
                user.setDifficulty(Difficulty.NORMAL);
                user.setExpMultiplier(Difficulty.NORMAL.getMultiplier());
            }
            case ORANGE_JASMINE -> {
                user.setJobClass(JobClass.DEALER);
                user.setElement(Element.FIRE);
                user.setDifficulty(Difficulty.HARD);
                user.setExpMultiplier(Difficulty.HARD.getMultiplier());
            }
        }
    }

    // ========== 파티 관리 ==========

    @GetMapping("/parties")
    public List<Map<String, Object>> getParties(Authentication authentication) {
        getAdminUser(authentication);
        List<Party> parties = partyRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Party p : parties) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("createdAt", p.getCreatedAt().toString());
            List<User> members = userRepository.findByPartyIdOrderByTotalDropsDesc(p.getId());
            map.put("memberCount", members.size());
            result.add(map);
        }
        return result;
    }

    @PostMapping("/parties")
    public Map<String, Object> createParty(@RequestBody Map<String, String> body, Authentication authentication) {
        getAdminUser(authentication);
        Party party = new Party();
        party.setName(body.get("name"));
        Party saved = partyRepository.save(party);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("status", "created");
        return result;
    }

    @PutMapping("/parties/{id}")
    public Map<String, String> updateParty(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication authentication) {
        getAdminUser(authentication);
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("파티를 찾을 수 없습니다."));
        party.setName(body.get("name"));
        partyRepository.save(party);
        return Map.of("status", "updated");
    }

    @DeleteMapping("/parties/{id}")
    public Map<String, String> deleteParty(@PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        partyRepository.deleteById(id);
        return Map.of("status", "deleted");
    }

    // ========== 통계 대시보드 ==========

    @GetMapping("/stats")
    public Map<String, Object> getStats(Authentication authentication) {
        getAdminUser(authentication);

        Map<String, Object> stats = new LinkedHashMap<>();

        // 전체 유저 수
        stats.put("totalUsers", userRepository.count());

        // 이번 달 통계
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 이번 달 글 작성 수
        stats.put("monthlyPosts", postRepository.count()); // 전체 글 수 (간단 버전)

        // 이번 달 물방울 발행 총량
        stats.put("monthlyDropsIssued", dropTransactionRepository.sumPositiveAmountByPeriod(monthStart, monthEnd));

        // 이번 달 거래 건수
        stats.put("monthlyTransactions", dropTransactionRepository.countByPeriod(monthStart, monthEnd));

        // 파티별 물방울
        List<Object[]> partyRankings = userRepository.getPartyRankings();
        List<Map<String, Object>> partyStats = new ArrayList<>();
        for (Object[] row : partyRankings) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("partyId", row[0]);
            entry.put("partyName", row[1]);
            entry.put("totalDrops", ((Number) row[2]).longValue());
            entry.put("memberCount", ((Number) row[3]).longValue());
            partyStats.add(entry);
        }
        stats.put("partyStats", partyStats);

        return stats;
    }

    // ========== 공지사항 ==========

    @PostMapping("/announcements")
    public Map<String, String> createAnnouncement(@RequestBody Map<String, String> body,
                                                   Authentication authentication) {
        getAdminUser(authentication);
        String title = body.get("title");
        String content = body.get("content");

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            notificationService.createNotification(user, NotificationType.ANNOUNCEMENT,
                    title, content, null, null);
        }

        return Map.of("status", "announced");
    }
}
