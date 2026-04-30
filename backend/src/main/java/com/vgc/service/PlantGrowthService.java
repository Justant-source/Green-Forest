package com.vgc.service;

import com.vgc.entity.*;
import com.vgc.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlantGrowthService {

    private static final Logger log = LoggerFactory.getLogger(PlantGrowthService.class);

    private static final int[] THRESHOLDS = {0, 20, 80, 200, 400, 700};
    private static final String[] LABELS = {"씨앗", "새싹", "잎2장", "꽃봉오리", "꽃", "열매"};

    private static final Map<GrowthScoreReason, Integer> DAILY_CAP = Map.of(
        GrowthScoreReason.LIKE_RECEIVED,    10,
        GrowthScoreReason.COMMENT_RECEIVED, 15,
        GrowthScoreReason.PRAISE_RECEIVED,  20,
        GrowthScoreReason.POST_CREATED,      6
    );

    private static final int MIN_POST_LENGTH_FOR_SCORE = 30;

    private final PlantGrowthRepository plantGrowthRepository;
    private final GrowthScoreLogRepository scoreLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OutboundEventService outboundEventService;

    public PlantGrowthService(PlantGrowthRepository plantGrowthRepository,
                              GrowthScoreLogRepository scoreLogRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              OutboundEventService outboundEventService) {
        this.plantGrowthRepository = plantGrowthRepository;
        this.scoreLogRepository = scoreLogRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.outboundEventService = outboundEventService;
    }

    // ────────────────────────────────────────────────
    // 공개 API
    // ────────────────────────────────────────────────

    @Transactional
    public void onAttendance(Long userId, Long checkinId) {
        recordScore(userId, 2, GrowthScoreReason.ATTENDANCE_DAILY, checkinId, false, null);
    }

    @Transactional
    public void onAttendanceStreak(Long userId, int streak, Long checkinId) {
        if (streak == 7)   recordScore(userId, 5,  GrowthScoreReason.ATTENDANCE_STREAK_7,   checkinId, true, null);
        if (streak == 30)  recordScore(userId, 15, GrowthScoreReason.ATTENDANCE_STREAK_30,  checkinId, true, null);
        if (streak == 100) recordScore(userId, 50, GrowthScoreReason.ATTENDANCE_STREAK_100, checkinId, true, null);
    }

    @Transactional
    public void onAttendanceDrawWin(Long userId, Long checkinId) {
        recordScore(userId, 10, GrowthScoreReason.ATTENDANCE_DRAW_WIN, checkinId, false, null);
    }

    @Transactional
    public void onPostCreated(Long authorId, Long postId, int contentLength) {
        if (contentLength < MIN_POST_LENGTH_FOR_SCORE) return;
        recordScore(authorId, 2, GrowthScoreReason.POST_CREATED, postId, false, GrowthScoreReason.POST_CREATED);
        recordScore(authorId, 5, GrowthScoreReason.ONBOARDING_FIRST_POST, postId, true, null);
    }

    @Transactional
    public void onLikeReceived(Long authorId, Long postId) {
        recordScore(authorId, 1, GrowthScoreReason.LIKE_RECEIVED, postId, false, GrowthScoreReason.LIKE_RECEIVED);
        PlantGrowth g = getOrCreate(authorId);
        g.setLikesReceived(g.getLikesReceived() + 1);
        plantGrowthRepository.save(g);
    }

    @Transactional
    public void onLikeRemoved(Long authorId, Long postId) {
        rollbackLastPositive(authorId, GrowthScoreReason.LIKE_RECEIVED, postId);
        PlantGrowth g = getOrCreate(authorId);
        g.setLikesReceived(Math.max(0, g.getLikesReceived() - 1));
        plantGrowthRepository.save(g);
    }

    /**
     * @param authorId   게시글 작성자(받는 쪽) ID
     * @param commentId  댓글 ID
     * @param commenterId 댓글 작성자(보내는 쪽) ID — ONBOARDING_FIRST_COMMENT 처리용
     */
    @Transactional
    public void onCommentReceived(Long authorId, Long commentId, Long commenterId) {
        // 자기 글에 자기가 댓글 단 경우 받는 쪽 점수는 skip, 온보딩 보너스는 항상 처리
        if (!authorId.equals(commenterId)) {
            recordScore(authorId, 3, GrowthScoreReason.COMMENT_RECEIVED, commentId, false, GrowthScoreReason.COMMENT_RECEIVED);
            PlantGrowth g = getOrCreate(authorId);
            g.setCommentsReceived(g.getCommentsReceived() + 1);
            plantGrowthRepository.save(g);
        }
        recordScore(commenterId, 5, GrowthScoreReason.ONBOARDING_FIRST_COMMENT, commentId, true, null);
    }

    @Transactional
    public void onCommentRemoved(Long authorId, Long commentId) {
        rollbackLastPositive(authorId, GrowthScoreReason.COMMENT_RECEIVED, commentId);
        PlantGrowth g = getOrCreate(authorId);
        g.setCommentsReceived(Math.max(0, g.getCommentsReceived() - 1));
        plantGrowthRepository.save(g);
    }

    @Transactional
    public void onPraiseReceived(Long taggedUserId, Long postId) {
        recordScore(taggedUserId, 10, GrowthScoreReason.PRAISE_RECEIVED, postId, false, GrowthScoreReason.PRAISE_RECEIVED);
        PlantGrowth g = getOrCreate(taggedUserId);
        g.setPraisesReceived(g.getPraisesReceived() + 1);
        plantGrowthRepository.save(g);
    }

    @Transactional
    public void onPraiseRevoked(Long taggedUserId, Long postId) {
        rollbackLastPositive(taggedUserId, GrowthScoreReason.PRAISE_RECEIVED, postId);
        PlantGrowth g = getOrCreate(taggedUserId);
        g.setPraisesReceived(Math.max(0, g.getPraisesReceived() - 1));
        plantGrowthRepository.save(g);
    }

    @Transactional
    public void onGachaWin(Long userId, Long drawId) {
        recordScore(userId, 5, GrowthScoreReason.GACHA_WIN, drawId, false, null);
    }

    // ────────────────────────────────────────────────
    // 조회 API
    // ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getMyGrowth(Long userId) {
        PlantGrowth growth = plantGrowthRepository.findByUserId(userId).orElse(null);
        int stage = growth != null ? growth.getStage() : 0;
        int score = growth != null ? growth.getGrowthScore() : 0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stage", stage);
        map.put("stageLabel", LABELS[stage]);
        map.put("score", score);
        map.put("nextStageScore", stage < THRESHOLDS.length - 1 ? THRESHOLDS[stage + 1] : -1);
        map.put("likesReceived", growth != null ? growth.getLikesReceived() : 0);
        map.put("commentsReceived", growth != null ? growth.getCommentsReceived() : 0);
        map.put("praisesReceived", growth != null ? growth.getPraisesReceived() : 0);
        map.put("lastGrownAt", growth != null && growth.getLastGrownAt() != null
                ? growth.getLastGrownAt().toString() : null);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        Map<String, Object> todayCaps = new LinkedHashMap<>();
        for (Map.Entry<GrowthScoreReason, Integer> e : DAILY_CAP.entrySet()) {
            int used = scoreLogRepository.sumDailyByReason(userId, e.getKey(), startOfDay, startOfNextDay);
            Map<String, Integer> info = new LinkedHashMap<>();
            info.put("used", Math.max(0, used));
            info.put("cap", e.getValue());
            todayCaps.put(e.getKey().name(), info);
        }
        map.put("todayCaps", todayCaps);

        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserGrowth(Long userId) {
        PlantGrowth growth = plantGrowthRepository.findByUserId(userId).orElse(null);
        int stage = growth != null ? growth.getStage() : 0;
        int score = growth != null ? growth.getGrowthScore() : 0;
        return Map.of("stage", stage, "stageLabel", LABELS[stage], "score", score);
    }

    // ────────────────────────────────────────────────
    // 내부 로직
    // ────────────────────────────────────────────────

    private void recordScore(Long userId, int delta, GrowthScoreReason reason,
                             Long refId, boolean lifetimeOnce,
                             GrowthScoreReason dailyCapReason) {
        if (delta == 0) return;

        if (lifetimeOnce && scoreLogRepository.existsByUserIdAndReason(userId, reason)) {
            return;
        }

        if (dailyCapReason != null && DAILY_CAP.containsKey(dailyCapReason)) {
            int cap = DAILY_CAP.get(dailyCapReason);
            LocalDate today = LocalDate.now();
            int alreadyToday = scoreLogRepository.sumDailyByReason(
                userId, dailyCapReason,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
            );
            if (alreadyToday >= cap) return;
            if (alreadyToday + delta > cap) delta = cap - alreadyToday;
            if (delta <= 0) return;
        }

        GrowthScoreLog logRow = new GrowthScoreLog();
        logRow.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId)));
        logRow.setScoreDelta(delta);
        logRow.setReason(reason);
        logRow.setRefId(refId);
        scoreLogRepository.save(logRow);

        PlantGrowth growth = getOrCreate(userId);
        growth.setGrowthScore(growth.getGrowthScore() + delta);
        recalculateAndCheckStageUp(growth);
        plantGrowthRepository.save(growth);
    }

    private void rollbackLastPositive(Long userId, GrowthScoreReason reason, Long refId) {
        List<GrowthScoreLog> positives = scoreLogRepository.findPositivesByUserReasonRef(userId, reason, refId);
        int negatives = scoreLogRepository.countNegativesByUserReasonRef(userId, reason, refId);
        if (positives.size() <= negatives) return;

        GrowthScoreLog target = positives.get(negatives);

        GrowthScoreLog rollback = new GrowthScoreLog();
        rollback.setUser(target.getUser());
        rollback.setScoreDelta(-target.getScoreDelta());
        rollback.setReason(reason);
        rollback.setRefId(refId);
        scoreLogRepository.save(rollback);

        PlantGrowth growth = getOrCreate(userId);
        growth.setGrowthScore(Math.max(0, growth.getGrowthScore() - target.getScoreDelta()));
        // stage 는 다운하지 않음 (점수만 줄어듦)
        plantGrowthRepository.save(growth);
    }

    private void recalculateAndCheckStageUp(PlantGrowth growth) {
        int newStage = calculateStage(growth.getGrowthScore());
        int oldStage = growth.getStage();
        if (newStage > oldStage) {
            growth.setStage(newStage);
            growth.setLastGrownAt(LocalDateTime.now());
            growth.setLastStageUpScore(growth.getGrowthScore());

            try {
                notificationService.createNotification(
                    growth.getUser(),
                    NotificationType.ANNOUNCEMENT,
                    "🌱 식물이 자랐어요!",
                    LABELS[newStage] + " 단계로 성장했어요!",
                    null, null
                );
            } catch (Exception e) {
                log.warn("STAGE_UP notification failed", e);
            }

            try {
                outboundEventService.publish("STAGE_UP", Map.of(
                    "userId", growth.getUser().getId(),
                    "userNickname", growth.getUser().getNickname(),
                    "newStage", newStage,
                    "stageLabel", LABELS[newStage],
                    "score", growth.getGrowthScore()
                ));
            } catch (Exception e) {
                log.error("STAGE_UP outbox publish failed", e);
            }
        }
    }

    private int calculateStage(int score) {
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= THRESHOLDS[i]) return i;
        }
        return 0;
    }

    private PlantGrowth getOrCreate(Long userId) {
        return plantGrowthRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            PlantGrowth g = new PlantGrowth();
            g.setUser(user);
            return g;
        });
    }
}
