package com.vgc.controller;

import com.vgc.entity.*;
import com.vgc.repository.PartyRepository;
import com.vgc.repository.UserRepository;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.PostRepository;
import com.vgc.repository.AttendanceCheckinRepository;
import com.vgc.repository.AttendancePhraseRepository;
import com.vgc.repository.GachaPrizeRepository;
import com.vgc.repository.GachaDrawRepository;
import com.vgc.repository.AnnouncementRepository;
import com.vgc.service.CategoryService;
import com.vgc.service.DropService;
import com.vgc.service.NotificationService;
import com.vgc.service.QuestService;
import com.vgc.service.AttendanceService;
import com.vgc.service.GachaService;
import com.vgc.service.ImageStorageService;
import com.vgc.dto.CategoryRequestResponse;
import com.vgc.dto.CategoryResponse;
import com.vgc.dto.AdminCreatePrizeRequest;
import com.vgc.dto.AdminUpdatePrizeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

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
    private final PasswordEncoder passwordEncoder;
    private final AttendanceCheckinRepository attendanceCheckinRepository;
    private final AttendancePhraseRepository attendancePhraseRepository;
    private final AttendanceService attendanceService;
    private final GachaPrizeRepository gachaPrizeRepository;
    private final GachaDrawRepository gachaDrawRepository;
    private final GachaService gachaService;
    private final AnnouncementRepository announcementRepository;
    private final ImageStorageService imageStorageService;

    public AdminController(CategoryService categoryService, UserRepository userRepository,
                           DropService dropService, QuestService questService,
                           PartyRepository partyRepository,
                           DropTransactionRepository dropTransactionRepository,
                           PostRepository postRepository,
                           NotificationService notificationService,
                           PasswordEncoder passwordEncoder,
                           AttendanceCheckinRepository attendanceCheckinRepository,
                           AttendancePhraseRepository attendancePhraseRepository,
                           AttendanceService attendanceService,
                           GachaPrizeRepository gachaPrizeRepository,
                           GachaDrawRepository gachaDrawRepository,
                           GachaService gachaService,
                           AnnouncementRepository announcementRepository,
                           ImageStorageService imageStorageService) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.dropService = dropService;
        this.questService = questService;
        this.partyRepository = partyRepository;
        this.dropTransactionRepository = dropTransactionRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.attendanceCheckinRepository = attendanceCheckinRepository;
        this.attendancePhraseRepository = attendancePhraseRepository;
        this.attendanceService = attendanceService;
        this.gachaPrizeRepository = gachaPrizeRepository;
        this.gachaDrawRepository = gachaDrawRepository;
        this.gachaService = gachaService;
        this.announcementRepository = announcementRepository;
        this.imageStorageService = imageStorageService;
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

    @GetMapping("/drops/history")
    public Page<Map<String, Object>> getUserDropHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication) {
        getAdminUser(authentication);
        return dropTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(tx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", tx.getId());
                    map.put("amount", tx.getAmount());
                    map.put("reasonType", tx.getReasonType().name());
                    map.put("reasonLabel", tx.getReasonType().getLabel());
                    map.put("reasonDetail", tx.getReasonDetail());
                    map.put("relatedPostId", tx.getRelatedPostId());
                    map.put("relatedQuestId", tx.getRelatedQuestId());
                    map.put("createdAt", tx.getCreatedAt().toString());
                    return map;
                });
    }

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
            map.put("name", u.getName());
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

        if (body.containsKey("plantType")) {
            if (body.get("plantType") == null) {
                // 관리자가 plantType을 null로 설정 → 잠금 해제 (유저가 재선택 가능)
                user.setPlantType(null);
                user.setPlantLocked(false);
                user.setJobClass(null);
                user.setElement(null);
                user.setDifficulty(null);
                user.setExpMultiplier(new java.math.BigDecimal("1.00"));
            } else {
                PlantType plantType = PlantType.valueOf((String) body.get("plantType"));
                user.setPlantType(plantType);
                user.setPlantLocked(true);
                // 식물→직업군 자동 매핑
                applyPlantJobMapping(user, plantType);
            }
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

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetUserPassword(@PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        String tempPassword = String.format("%04d", new java.util.Random().nextInt(10000));
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        return Map.of("status", "reset", "tempPassword", tempPassword);
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

    @GetMapping("/announcements")
    public ResponseEntity<List<com.vgc.entity.Announcement>> listAnnouncements(Authentication authentication) {
        getAdminUser(authentication);
        return ResponseEntity.ok(announcementRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/announcements")
    public ResponseEntity<com.vgc.entity.Announcement> createAnnouncement(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        getAdminUser(authentication);
        com.vgc.entity.Announcement ann = new com.vgc.entity.Announcement();
        ann.setTitle(body.get("title"));
        ann.setContent(body.get("content"));
        ann.setActive(false);
        return ResponseEntity.ok(announcementRepository.save(ann));
    }

    @PatchMapping("/announcements/{id}/activate")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<com.vgc.entity.Announcement> activateAnnouncement(
            @PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        announcementRepository.deactivateAll();
        com.vgc.entity.Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지 없음"));
        ann.setActive(true);
        return ResponseEntity.ok(announcementRepository.save(ann));
    }

    @DeleteMapping("/announcements/deactivate-all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deactivateAllAnnouncements(Authentication authentication) {
        getAdminUser(authentication);
        announcementRepository.deactivateAll();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/announcements/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long id, Authentication authentication) {
        getAdminUser(authentication);
        announcementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ===== 출석 관리 =====

    @GetMapping("/attendance/stats")
    public ResponseEntity<Map<String, Object>> attendanceStats(
            @RequestParam String from, @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        long total = attendanceCheckinRepository.countByCheckinDateBetween(fromDate, toDate);
        List<Object[]> topUsers = attendanceCheckinRepository.findTopAttendanceUsers(fromDate, toDate);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCheckins", total);
        result.put("from", from);
        result.put("to", to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attendance/ranking")
    public ResponseEntity<List<Map<String, Object>>> attendanceRanking(
            @RequestParam String from, @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        List<Object[]> rows = attendanceCheckinRepository.findTopAttendanceUsers(fromDate, toDate);
        List<Map<String, Object>> ranking = rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", row[0]);
            m.put("nickname", row[1]);
            m.put("count", row[2]);
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ranking);
    }

    @PostMapping("/attendance/draw-winner")
    public ResponseEntity<Map<String, Object>> drawWinner(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return ResponseEntity.ok(attendanceService.drawDailyWinner(d));
    }

    @GetMapping("/attendance/winners")
    public ResponseEntity<List<Map<String, Object>>> getWinners(
            @RequestParam String from, @RequestParam String to) {
        List<AttendanceCheckin> winners = attendanceCheckinRepository.findWinnersBetween(
                LocalDate.parse(from), LocalDate.parse(to));
        List<Map<String, Object>> result = winners.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", c.getCheckinDate().toString());
            m.put("userId", c.getUser().getId());
            m.put("nickname", c.getUser().getNickname());
            m.put("email", c.getUser().getEmail());
            m.put("checkinAt", c.getCheckinAt().toString());
            m.put("message", c.getMessage());
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/attendance/phrases")
    public ResponseEntity<AttendancePhrase> createPhrase(@RequestBody Map<String, String> body) {
        AttendancePhrase p = new AttendancePhrase();
        p.setPhrase(body.get("phrase"));
        p.setCategory(body.getOrDefault("category", "GENERAL"));
        return ResponseEntity.ok(attendancePhraseRepository.save(p));
    }

    @PutMapping("/attendance/phrases/{id}")
    public ResponseEntity<AttendancePhrase> updatePhrase(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AttendancePhrase p = attendancePhraseRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (body.containsKey("phrase")) p.setPhrase((String) body.get("phrase"));
        if (body.containsKey("category")) p.setCategory((String) body.get("category"));
        if (body.containsKey("active")) p.setActive((Boolean) body.get("active"));
        return ResponseEntity.ok(attendancePhraseRepository.save(p));
    }

    @DeleteMapping("/attendance/phrases/{id}")
    public ResponseEntity<Void> deletePhrase(@PathVariable Long id) {
        attendancePhraseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attendance/phrases")
    public ResponseEntity<List<AttendancePhrase>> listPhrases() {
        return ResponseEntity.ok(attendancePhraseRepository.findAll());
    }

    // ===== 뽑기 관리 =====

    @PostMapping(value = "/gacha/prizes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GachaPrize> createGachaPrize(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam int cashValue,
            @RequestParam int totalStock,
            @RequestParam String tier,
            @RequestParam(required = false, defaultValue = "1.00") String evMultiplier,
            @RequestParam(required = false, defaultValue = "0") int displayOrder,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        AdminCreatePrizeRequest req = new AdminCreatePrizeRequest();
        req.setName(name);
        req.setDescription(description);
        req.setCashValue(cashValue);
        req.setTotalStock(totalStock);
        req.setTier(com.vgc.entity.GachaPrizeTier.valueOf(tier));
        req.setEvMultiplier(new BigDecimal(evMultiplier));
        req.setDisplayOrder(displayOrder);
        GachaPrize prize = gachaService.createPrize(req);
        if (image != null && !image.isEmpty()) {
            gachaService.uploadAndSetPrizeImage(prize.getId(), image.getBytes(), image.getOriginalFilename());
        }
        return ResponseEntity.ok(prize);
    }

    @PutMapping(value = "/gacha/prizes/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<GachaPrize> updateGachaPrize(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer cashValue,
            @RequestParam(required = false) Integer totalStock,
            @RequestParam(required = false) Integer remainingStock,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String evMultiplier,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        AdminUpdatePrizeRequest req = new AdminUpdatePrizeRequest();
        req.setName(name);
        req.setDescription(description);
        req.setCashValue(cashValue);
        req.setTotalStock(totalStock);
        req.setRemainingStock(remainingStock);
        if (tier != null) req.setTier(com.vgc.entity.GachaPrizeTier.valueOf(tier));
        if (evMultiplier != null) req.setEvMultiplier(new BigDecimal(evMultiplier));
        req.setActive(active);
        req.setDisplayOrder(displayOrder);
        GachaPrize prize = gachaService.updatePrize(id, req);
        if (image != null && !image.isEmpty()) {
            gachaService.uploadAndSetPrizeImage(id, image.getBytes(), image.getOriginalFilename());
        }
        return ResponseEntity.ok(prize);
    }

    @DeleteMapping("/gacha/prizes/{id}")
    public ResponseEntity<Void> deactivateGachaPrize(@PathVariable Long id) {
        gachaService.deactivatePrize(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/gacha/prizes")
    public ResponseEntity<List<Map<String, Object>>> listAllGachaPrizes() {
        return ResponseEntity.ok(gachaService.listAllPrizes());
    }

    @GetMapping("/gacha/deliveries")
    public ResponseEntity<List<com.vgc.dto.AdminDeliveryDto>> listDeliveries(
            @RequestParam(defaultValue = "PENDING") String status) {
        com.vgc.entity.GachaDeliveryStatus deliveryStatus =
                com.vgc.entity.GachaDeliveryStatus.valueOf(status);
        return ResponseEntity.ok(gachaService.listDeliveries(deliveryStatus));
    }

    @PatchMapping("/gacha/deliveries/{drawId}/deliver")
    public ResponseEntity<com.vgc.dto.AdminDeliveryDto> markDelivered(
            @PathVariable Long drawId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User admin = getAdminUser(authentication);
        String memo = body != null ? body.get("memo") : null;
        return ResponseEntity.ok(gachaService.markDelivered(drawId, admin.getId(), memo));
    }

    @GetMapping("/gacha/stats")
    public ResponseEntity<Map<String, Object>> gachaStats(
            @RequestParam String from, @RequestParam String to) {
        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt = LocalDate.parse(to).atTime(23, 59, 59);
        return ResponseEntity.ok(gachaService.getStats(fromDt, toDt));
    }

    @GetMapping("/gacha/simulate")
    public ResponseEntity<Map<String, Object>> simulateEv(
            @RequestParam Long prizeId, @RequestParam java.math.BigDecimal ev) {
        return ResponseEntity.ok(gachaService.simulateEv(prizeId, ev));
    }
}
