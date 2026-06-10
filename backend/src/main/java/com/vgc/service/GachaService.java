package com.vgc.service;

import com.vgc.dto.AdminCreatePrizeRequest;
import com.vgc.dto.AdminUpdatePrizeRequest;
import com.vgc.dto.GachaStatsDto;
import com.vgc.entity.*;
import com.vgc.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GachaService {

    private static final int DRAW_COST = 30;
    private static final int DAILY_DRAW_LIMIT = 3;
    private static final int ADMIN_DAILY_DRAW_LIMIT = 500;
    private static final BigDecimal DROP_CASH_VALUE = new BigDecimal("20");
    private static final BigDecimal MAX_EV_MULTIPLIER = new BigDecimal("1.50");
    private static final BigDecimal MIN_EV_MULTIPLIER = new BigDecimal("1.00");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_PITY_STACKS = 100;
    private static final BigDecimal BONUS_RATE_PER_UNIT = new BigDecimal("0.10"); // 스택·weight 1당 기본확률의 10%

    private final GachaPrizeRepository prizeRepository;
    private final GachaDrawRepository drawRepository;
    private final UserRepository userRepository;
    private final DropService dropService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final ImageStorageService imageStorageService;
    private final OutboundEventService outboundEventService;
    private final PlantGrowthService plantGrowthService;
    private final GachaPityStackRepository pityStackRepository;
    private final AttendanceCheckinRepository attendanceCheckinRepository;
    private final PostRepository postRepository;

    public GachaService(GachaPrizeRepository prizeRepository,
                        GachaDrawRepository drawRepository,
                        UserRepository userRepository,
                        DropService dropService,
                        NotificationService notificationService,
                        ActivityLogService activityLogService,
                        ImageStorageService imageStorageService,
                        OutboundEventService outboundEventService,
                        PlantGrowthService plantGrowthService,
                        GachaPityStackRepository pityStackRepository,
                        AttendanceCheckinRepository attendanceCheckinRepository,
                        PostRepository postRepository) {
        this.prizeRepository = prizeRepository;
        this.drawRepository = drawRepository;
        this.userRepository = userRepository;
        this.dropService = dropService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.imageStorageService = imageStorageService;
        this.outboundEventService = outboundEventService;
        this.plantGrowthService = plantGrowthService;
        this.pityStackRepository = pityStackRepository;
        this.attendanceCheckinRepository = attendanceCheckinRepository;
        this.postRepository = postRepository;
    }

    @Async
    @Transactional
    public void uploadAndSetPrizeImage(Long prizeId, byte[] imageBytes, String originalFilename) {
        try {
            String imageUrl = imageStorageService.uploadBytes(imageBytes, originalFilename);
            GachaPrize prize = prizeRepository.findById(prizeId).orElse(null);
            if (prize != null) {
                prize.setImageUrl(imageUrl);
                prizeRepository.save(prize);
            }
        } catch (IOException e) {
            // 업로드 실패 시 무시 (이미지 없이 상품은 정상 저장됨)
        }
    }

    @Transactional
    public Map<String, Object> draw(Long userId, Long prizeId) {
        // 비관적 락으로 prize, user 조회
        GachaPrize prize = prizeRepository.findForUpdateById(prizeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // 검증
        if (!prize.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비활성 상품입니다");
        }
        if (prize.getRemainingStock() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "재고가 없습니다");
        }
        if (user.getTotalDrops() < DRAW_COST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "물방울이 부족합니다 (30 필요)");
        }

        // 오늘 뽑기 횟수 체크 (ADMIN은 제한 없음)
        ZonedDateTime kstNow = ZonedDateTime.now(KST);
        LocalDateTime todayStart = kstNow.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayCount = drawRepository.countByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
        boolean isAdmin = "ADMIN".equals(user.getRole());
        int effectiveDailyLimit = isAdmin ? ADMIN_DAILY_DRAW_LIMIT : DAILY_DRAW_LIMIT;
        if (todayCount >= effectiveDailyLimit) {
            activityLogService.logGachaLimitExceeded(userId, user.getNickname());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "오늘 뽑기 횟수(" + effectiveDailyLimit + "회)를 초과했습니다");
        }

        // 비관적 락으로 pity 스택 조회 (없으면 신규 생성)
        GachaPityStack pityStack = pityStackRepository.findForUpdateByUserIdAndPrizeId(userId, prizeId)
                .orElseGet(() -> {
                    GachaPityStack s = new GachaPityStack();
                    s.setUser(user);
                    s.setPrize(prize);
                    s.setStackCount(0);
                    return s;
                });

        // 확률 분해 계산 (표시 확률 = 판정 확률)
        ProbabilityBreakdown breakdown = computeProbabilityBreakdown(userId, prize, pityStack.getStackCount());

        // 난수
        BigDecimal rng = new BigDecimal(secureRandom.nextDouble()).setScale(5, RoundingMode.HALF_UP);
        boolean isWinner = rng.compareTo(breakdown.total) < 0;

        // 재고 처리 및 pity 스택 갱신
        if (isWinner) {
            prize.setRemainingStock(prize.getRemainingStock() - 1);
            prizeRepository.save(prize);
            // 당첨 시 스택 50% 차감 (floor)
            pityStack.setStackCount((int) Math.floor(pityStack.getStackCount() * 0.5));
        } else {
            // 꽝 시 스택 +1 (최대 100)
            pityStack.setStackCount(Math.min(pityStack.getStackCount() + 1, MAX_PITY_STACKS));
        }
        pityStackRepository.save(pityStack);

        // 기록 저장
        GachaDraw draw = new GachaDraw();
        draw.setUser(user);
        draw.setPrize(prize);
        draw.setPrizeName(prize.getName());
        draw.setPrizeCashValue(prize.getCashValue());
        draw.setDropsSpent(DRAW_COST);
        draw.setWinProbability(breakdown.total);
        draw.setRngValue(rng);
        draw.setWinner(isWinner);
        draw.setDeliveryStatus(isWinner ? GachaDeliveryStatus.PENDING : GachaDeliveryStatus.NONE);
        draw.setBaseProbability(breakdown.base);
        draw.setPityBonus(breakdown.pityBonus);
        draw.setPityStacksAtDraw(breakdown.pityStacks);
        draw.setActivityBonus(breakdown.activityBonus);
        GachaDraw saved = drawRepository.save(draw);

        // 물방울 차감 (draw 저장 후 drawId 포함)
        dropService.deductForGacha(user, DRAW_COST, prize.getName(), saved.getId(), isWinner);
        activityLogService.logGachaDraw(userId, user.getNickname(), prize.getName(), isWinner, saved.getId());

        // 당첨 알림
        if (isWinner) {
            try {
                notificationService.createNotification(user, NotificationType.DROP_AWARD,
                        "🎊 뽑기 당첨!",
                        "[" + prize.getName() + "] 당첨되었습니다! 관리자에게 문의하세요.",
                        null, null);
            } catch (Exception ignored) {}

            // 사내망 polling 용 outbox publish
            // 누가/언제/어떤 상품을/어떤 확률로 도전해 당첨됐는지 모두 실어 보낸다.
            Map<String, Object> gachaPayload = new LinkedHashMap<>();
            gachaPayload.put("drawId", saved.getId());
            gachaPayload.put("userId", user.getId());
            gachaPayload.put("userNickname", user.getNickname());
            gachaPayload.put("prizeId", prize.getId());
            gachaPayload.put("prizeName", prize.getName());
            gachaPayload.put("prizeCashValue", prize.getCashValue());
            gachaPayload.put("prizeTier", prize.getTier().name());
            gachaPayload.put("evMultiplier", prize.getEvMultiplier());
            gachaPayload.put("winProbability", breakdown.total);
            gachaPayload.put("rngValue", rng);
            gachaPayload.put("dropsSpent", DRAW_COST);
            gachaPayload.put("remainingStock", prize.getRemainingStock());
            gachaPayload.put("drawnAt", saved.getCreatedAt() != null
                    ? saved.getCreatedAt().toString()
                    : LocalDateTime.now(KST).toString());
            gachaPayload.put("todayDrawNumber", todayCount + 1);
            gachaPayload.put("dailyDrawLimit", DAILY_DRAW_LIMIT);
            outboundEventService.publish("GACHA_WIN", gachaPayload);
            plantGrowthService.onGachaWin(user.getId(), saved.getId());
        }

        long remainingToday = Math.max(0, effectiveDailyLimit - todayCount - 1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("drawId", saved.getId());
        result.put("isWinner", isWinner);
        result.put("probability", breakdown.total);
        result.put("rngValue", rng);
        result.put("prizeName", prize.getName());
        result.put("prizeImageUrl", prize.getImageUrl());
        result.put("prizeCashValue", prize.getCashValue());
        result.put("remainingDrawsToday", remainingToday);
        result.put("breakdown", breakdownToMap(breakdown));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAvailablePrizes() {
        return prizeRepository.findByActiveTrueAndRemainingStockGreaterThanOrderByDisplayOrderAscIdAsc(0)
                .stream().map(this::toPrizeResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAvailablePrizesForUser(Long userId) {
        return prizeRepository.findByActiveTrueAndRemainingStockGreaterThanOrderByDisplayOrderAscIdAsc(0)
                .stream().map(p -> toPrizeResponseForUser(p, userId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<GachaDraw> getMyHistory(Long userId, Pageable pageable) {
        return drawRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentWins(int limit) {
        if (limit > 20) limit = 20;
        return drawRepository.findTop20ByWinnerTrueOrderByCreatedAtDesc().stream()
                .limit(limit)
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("drawId", d.getId());
                    m.put("userNickname", d.getUser().getNickname());
                    m.put("prizeName", d.getPrizeName());
                    m.put("probability", d.getWinProbability());
                    m.put("createdAt", d.getCreatedAt().toString());
                    return m;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRemainingDrawsToday(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        ZonedDateTime kstNow = ZonedDateTime.now(KST);
        LocalDateTime todayStart = kstNow.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long usedToday = drawRepository.countByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
        boolean isAdmin = "ADMIN".equals(user.getRole());
        int effectiveDailyLimit = isAdmin ? ADMIN_DAILY_DRAW_LIMIT : DAILY_DRAW_LIMIT;
        long remaining = Math.max(0, effectiveDailyLimit - usedToday);
        return Map.of("remainingToday", remaining, "limit", effectiveDailyLimit);
    }

    @Transactional
    public GachaPrize createPrize(AdminCreatePrizeRequest req) {
        validateEv(req.getEvMultiplier());
        GachaPrize prize = new GachaPrize();
        prize.setName(req.getName());
        prize.setDescription(req.getDescription());
        prize.setImageUrl(req.getImageUrl());
        prize.setCashValue(req.getCashValue());
        prize.setTotalStock(req.getTotalStock());
        prize.setRemainingStock(req.getTotalStock());
        prize.setTier(req.getTier());
        prize.setEvMultiplier(req.getEvMultiplier() != null ? req.getEvMultiplier() : req.getTier().getDefaultEvMultiplier());
        prize.setDisplayOrder(req.getDisplayOrder());
        return prizeRepository.save(prize);
    }

    @Transactional
    public GachaPrize updatePrize(Long id, AdminUpdatePrizeRequest req) {
        GachaPrize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음"));
        if (req.getName() != null) prize.setName(req.getName());
        if (req.getDescription() != null) prize.setDescription(req.getDescription());
        if (req.getImageUrl() != null) prize.setImageUrl(req.getImageUrl());
        if (req.getCashValue() != null) prize.setCashValue(req.getCashValue());
        if (req.getTotalStock() != null) prize.setTotalStock(req.getTotalStock());
        if (req.getRemainingStock() != null) prize.setRemainingStock(req.getRemainingStock());
        if (req.getEvMultiplier() != null) {
            validateEv(req.getEvMultiplier());
            prize.setEvMultiplier(req.getEvMultiplier());
        }
        if (req.getActive() != null) prize.setActive(req.getActive());
        if (req.getDisplayOrder() != null) prize.setDisplayOrder(req.getDisplayOrder());
        if (req.getTier() != null) prize.setTier(req.getTier());
        return prizeRepository.save(prize);
    }

    @Transactional
    public void deactivatePrize(Long id) {
        GachaPrize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음"));
        prize.setActive(false);
        prizeRepository.save(prize);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllPrizes() {
        return prizeRepository.findAll(org.springframework.data.domain.Sort.by("displayOrder")).stream()
                .map(p -> {
                    Map<String, Object> m = toPrizeResponse(p);
                    m.put("active", p.isActive());
                    m.put("evMultiplier", p.getEvMultiplier());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.vgc.dto.AdminDeliveryDto> listDeliveries(GachaDeliveryStatus status) {
        return drawRepository.findWinnersWithUserByStatus(status).stream()
                .map(com.vgc.dto.AdminDeliveryDto::from)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public com.vgc.dto.AdminDeliveryDto markDelivered(Long drawId, Long adminId, String memo) {
        GachaDraw draw = drawRepository.findById(drawId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기록 없음"));
        draw.setDeliveryStatus(GachaDeliveryStatus.DELIVERED);
        draw.setDeliveredAt(LocalDateTime.now(KST));
        draw.setDeliveredBy(adminId);
        draw.setDeliveryMemo(memo);
        GachaDraw saved = drawRepository.save(draw);
        return com.vgc.dto.AdminDeliveryDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(LocalDateTime from, LocalDateTime to) {
        Object[] stats = drawRepository.findStats(from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDraws", stats[0]);
        result.put("totalWins", stats[1]);
        result.put("totalCashValue", stats[2]);
        return result;
    }

    @Transactional(readOnly = true)
    public GachaStatsDto getStats(Long userId) {
        // 오늘 뽑기 횟수
        ZonedDateTime kstNow = ZonedDateTime.now(KST);
        LocalDateTime todayStart = kstNow.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayCount = drawRepository.countByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
        User statUser = userRepository.findById(userId).orElseThrow();
        boolean isAdmin = "ADMIN".equals(statUser.getRole());
        int effectiveDailyLimit = isAdmin ? ADMIN_DAILY_DRAW_LIMIT : DAILY_DRAW_LIMIT;
        long remaining = Math.max(0, effectiveDailyLimit - todayCount);

        // 활성 상품 목록
        List<GachaPrize> activePrizes = prizeRepository.findByActiveTrueAndRemainingStockGreaterThanOrderByDisplayOrderAscIdAsc(0);
        int totalActivePrizes = activePrizes.size();

        // 기대값 계산: 각 상품의 확률 가중평균
        BigDecimal expectedReward = BigDecimal.ZERO;
        if (!activePrizes.isEmpty()) {
            // 모든 활성 상품의 weight 합계
            BigDecimal totalWeight = activePrizes.stream()
                    .map(p -> p.getEvMultiplier())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 각 상품의 기대값: (cash_value * ev) * (ev / total_weight)
            for (GachaPrize p : activePrizes) {
                BigDecimal baseProb = new BigDecimal(DRAW_COST).multiply(DROP_CASH_VALUE)
                        .divide(new BigDecimal(p.getCashValue()), 5, RoundingMode.HALF_UP);
                BigDecimal finalProb = baseProb.multiply(p.getEvMultiplier()).min(BigDecimal.ONE);
                // 기대 보상 = cashValue * 승률
                BigDecimal prizeValue = new BigDecimal(p.getCashValue()).multiply(finalProb);
                expectedReward = expectedReward.add(prizeValue);
            }
            // 평균
            if (!activePrizes.isEmpty()) {
                expectedReward = expectedReward.divide(
                        new BigDecimal(activePrizes.size()),
                        2, RoundingMode.HALF_UP);
            }
        }

        return new GachaStatsDto(remaining, effectiveDailyLimit, todayCount, DRAW_COST, expectedReward, totalActivePrizes);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDrawHistory(String nickname, LocalDateTime from, LocalDateTime to,
                                                    Long prizeId, boolean winnerOnly, int page, int size, User admin) {
        if (!"ADMIN".equals(admin.getRole()))
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "관리자만 접근 가능합니다");

        String nicknameParam = (nickname == null || nickname.isBlank()) ? null : nickname;
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        Page<GachaDraw> drawPage = drawRepository.findAdminDraws(nicknameParam, from, to, prizeId, winnerOnly, pageable);

        long totalForStats = drawRepository.countTotalForStats(nicknameParam, from, to, prizeId);
        long winsForStats  = drawRepository.countWinsForStats(nicknameParam, from, to, prizeId);
        BigDecimal actualWinRate = totalForStats == 0 ? BigDecimal.ZERO :
                new BigDecimal(winsForStats).divide(new BigDecimal(totalForStats), 5, RoundingMode.HALF_UP);

        List<Map<String, Object>> items = drawPage.getContent().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("userNickname", d.getUser().getNickname());
            m.put("prizeName", d.getPrizeName());
            m.put("prizeCashValue", d.getPrizeCashValue());
            m.put("dropsSpent", d.getDropsSpent());
            m.put("winProbability", d.getWinProbability());
            m.put("rngValue", d.getRngValue());
            m.put("isWinner", d.isWinner());
            m.put("deliveryStatus", d.getDeliveryStatus().name());
            m.put("createdAt", d.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalElements", drawPage.getTotalElements());
        result.put("totalPages", drawPage.getTotalPages());
        result.put("page", drawPage.getNumber());
        result.put("size", drawPage.getSize());
        result.put("totalDrawsInFilter", totalForStats);
        result.put("totalWinsInFilter", winsForStats);
        result.put("actualWinRate", actualWinRate);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> simulateEv(Long prizeId, BigDecimal simulatedEv) {
        validateEv(simulatedEv);
        GachaPrize prize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음"));
        BigDecimal baseProb = new BigDecimal(DRAW_COST).multiply(DROP_CASH_VALUE)
                .divide(new BigDecimal(prize.getCashValue()), 5, RoundingMode.HALF_UP);
        BigDecimal simProb = baseProb.multiply(simulatedEv).min(BigDecimal.ONE).setScale(5, RoundingMode.HALF_UP);
        long expectedDraws = simProb.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                BigDecimal.ONE.divide(simProb, 0, RoundingMode.CEILING).longValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prizeId", prize.getId());
        result.put("prizeName", prize.getName());
        result.put("cashValue", prize.getCashValue());
        result.put("currentEv", prize.getEvMultiplier());
        result.put("simulatedEv", simulatedEv);
        result.put("baseProbability", baseProb);
        result.put("simulatedProbability", simProb);
        result.put("expectedDrawsToWin", expectedDraws);
        return result;
    }

    private void validateEv(BigDecimal ev) {
        if (ev == null) return;
        if (ev.compareTo(MIN_EV_MULTIPLIER) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EV는 1.00 이상이어야 합니다");
        }
        if (ev.compareTo(MAX_EV_MULTIPLIER) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EV는 1.50을 초과할 수 없습니다");
        }
    }

    private Map<String, Object> toPrizeResponse(GachaPrize p) {
        BigDecimal baseProb = new BigDecimal(DRAW_COST).multiply(DROP_CASH_VALUE)
                .divide(new BigDecimal(p.getCashValue()), 5, RoundingMode.HALF_UP);
        BigDecimal finalProb = baseProb.multiply(p.getEvMultiplier()).min(BigDecimal.ONE).setScale(5, RoundingMode.HALF_UP);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("imageUrl", p.getImageUrl());
        m.put("cashValue", p.getCashValue());
        m.put("remainingStock", p.getRemainingStock());
        m.put("tier", p.getTier().name());
        m.put("tierLabel", p.getTier().getLabel());
        m.put("currentProbability", finalProb);
        m.put("displayOrder", p.getDisplayOrder());
        return m;
    }

    private Map<String, Object> toPrizeResponseForUser(GachaPrize p, Long userId) {
        int stackCount = pityStackRepository.findByUserIdAndPrizeId(userId, p.getId())
                .map(GachaPityStack::getStackCount).orElse(0);
        ProbabilityBreakdown breakdown = computeProbabilityBreakdown(userId, p, stackCount);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("imageUrl", p.getImageUrl());
        m.put("cashValue", p.getCashValue());
        m.put("remainingStock", p.getRemainingStock());
        m.put("tier", p.getTier().name());
        m.put("tierLabel", p.getTier().getLabel());
        m.put("currentProbability", breakdown.total);
        m.put("displayOrder", p.getDisplayOrder());
        m.put("probabilityBreakdown", breakdownToMap(breakdown));
        return m;
    }

    // 확률 분해 계산 (draw()와 목록 표시 양쪽에서 동일 로직 사용)
    private ProbabilityBreakdown computeProbabilityBreakdown(Long userId, GachaPrize prize, int stackCount) {
        // 기본 확률 (기존 공식 그대로)
        BigDecimal base = new BigDecimal(DRAW_COST).multiply(DROP_CASH_VALUE)
                .divide(new BigDecimal(prize.getCashValue()), 5, RoundingMode.HALF_UP)
                .multiply(prize.getEvMultiplier().min(MAX_EV_MULTIPLIER))
                .min(BigDecimal.ONE)
                .setScale(5, RoundingMode.HALF_UP);

        // 스택 보너스: base × 10% × 스택수
        BigDecimal pityBonus = base.multiply(BONUS_RATE_PER_UNIT)
                .multiply(new BigDecimal(stackCount))
                .setScale(5, RoundingMode.HALF_UP);

        // 활동 보너스
        ZonedDateTime kstNow = ZonedDateTime.now(KST);
        LocalDate todayKST = kstNow.toLocalDate();
        int totalWeights = 0;
        List<Map<String, Object>> factors = new ArrayList<>();

        // 출석 (weight 1)
        if (attendanceCheckinRepository.findByUserIdAndCheckinDate(userId, todayKST).isPresent()) {
            totalWeights += 1;
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("label", "출석");
            f.put("weight", 1);
            f.put("bonus", base.multiply(BONUS_RATE_PER_UNIT).setScale(5, RoundingMode.HALF_UP));
            factors.add(f);
        }

        // 주간 칭찬글 (weight 4)
        if (dropService.hasPraisedThisWeek(userId)) {
            totalWeights += 4;
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("label", "주간 칭찬글");
            f.put("weight", 4);
            f.put("bonus", base.multiply(BONUS_RATE_PER_UNIT).multiply(new BigDecimal(4)).setScale(5, RoundingMode.HALF_UP));
            factors.add(f);
        }

        // 오늘 게시글 작성 (weight 1, survey 제외)
        LocalDateTime todayStart = todayKST.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        if (postRepository.existsByAuthorIdAndCategoryNotAndCreatedAtBetween(userId, "survey", todayStart, todayEnd)) {
            totalWeights += 1;
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("label", "오늘 게시글");
            f.put("weight", 1);
            f.put("bonus", base.multiply(BONUS_RATE_PER_UNIT).setScale(5, RoundingMode.HALF_UP));
            factors.add(f);
        }

        int cappedWeights = Math.min(totalWeights, 6);
        BigDecimal activityBonus = base.multiply(BONUS_RATE_PER_UNIT)
                .multiply(new BigDecimal(cappedWeights))
                .setScale(5, RoundingMode.HALF_UP);

        BigDecimal total = base.add(pityBonus).add(activityBonus)
                .min(BigDecimal.ONE)
                .setScale(5, RoundingMode.HALF_UP);

        return new ProbabilityBreakdown(base, pityBonus, stackCount, activityBonus, factors, total);
    }

    private Map<String, Object> breakdownToMap(ProbabilityBreakdown bd) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("base", bd.base);
        m.put("pityBonus", bd.pityBonus);
        m.put("pityStacks", bd.pityStacks);
        m.put("activityBonus", bd.activityBonus);
        m.put("factors", bd.factors);
        m.put("total", bd.total);
        return m;
    }

    // 확률 분해 내부 DTO
    private record ProbabilityBreakdown(
            BigDecimal base,
            BigDecimal pityBonus,
            int pityStacks,
            BigDecimal activityBonus,
            List<Map<String, Object>> factors,
            BigDecimal total
    ) {}
}
