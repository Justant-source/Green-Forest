package com.vgc.service;

import com.vgc.entity.*;
import com.vgc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantGrowthServiceTest {

    @Mock private PlantGrowthRepository plantGrowthRepository;
    @Mock private GrowthScoreLogRepository scoreLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private OutboundEventService outboundEventService;

    @InjectMocks
    private PlantGrowthService sut;

    private User user;
    private PlantGrowth growth;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setNickname("테스터");

        growth = new PlantGrowth();
        growth.setUser(user);
        growth.setStage(0);
        growth.setGrowthScore(0);
        growth.setLikesReceived(0);
        growth.setCommentsReceived(0);
        growth.setPraisesReceived(0);
        growth.setLastStageUpScore(0);

        when(plantGrowthRepository.findByUserId(1L)).thenReturn(Optional.of(growth));
        when(plantGrowthRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(scoreLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ────────────────────────────────────────────────
    // 테스트 1: 좋아요 일일 캡 10점
    // ────────────────────────────────────────────────
    @Test
    void likeDailyCap_stops_at_10() {
        // 오늘 이미 10점 적립됨 → 11번째는 무시
        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.LIKE_RECEIVED))
            .thenReturn(false);
        when(scoreLogRepository.sumDailyByReason(
                eq(1L), eq(GrowthScoreReason.LIKE_RECEIVED), any(), any()))
            .thenReturn(10); // 이미 캡 도달

        sut.onLikeReceived(1L, 100L);

        // 캡 도달했으므로 로그 저장 안 됨
        verify(scoreLogRepository, never()).save(any(GrowthScoreLog.class));
        assertThat(growth.getGrowthScore()).isEqualTo(0);
    }

    // ────────────────────────────────────────────────
    // 테스트 2: 좋아요 추가 → 취소 → 점수 0
    // ────────────────────────────────────────────────
    @Test
    void likeReceived_then_removed_results_in_zero() {
        // 좋아요 추가
        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.LIKE_RECEIVED))
            .thenReturn(false);
        when(scoreLogRepository.sumDailyByReason(
                eq(1L), eq(GrowthScoreReason.LIKE_RECEIVED), any(), any()))
            .thenReturn(0);

        sut.onLikeReceived(1L, 200L);
        assertThat(growth.getGrowthScore()).isEqualTo(1);

        // 좋아요 취소 — rollback
        GrowthScoreLog positiveLog = new GrowthScoreLog();
        positiveLog.setUser(user);
        positiveLog.setScoreDelta(1);
        positiveLog.setReason(GrowthScoreReason.LIKE_RECEIVED);
        positiveLog.setRefId(200L);
        positiveLog.setCreatedAt(LocalDateTime.now().minusSeconds(10));

        when(scoreLogRepository.findPositivesByUserReasonRef(1L, GrowthScoreReason.LIKE_RECEIVED, 200L))
            .thenReturn(List.of(positiveLog));
        when(scoreLogRepository.countNegativesByUserReasonRef(1L, GrowthScoreReason.LIKE_RECEIVED, 200L))
            .thenReturn(0);

        sut.onLikeRemoved(1L, 200L);
        assertThat(growth.getGrowthScore()).isEqualTo(0);
    }

    // ────────────────────────────────────────────────
    // 테스트 3: rollback 중복 방지
    // ────────────────────────────────────────────────
    @Test
    void rollback_duplicate_is_noop() {
        GrowthScoreLog positiveLog = new GrowthScoreLog();
        positiveLog.setUser(user);
        positiveLog.setScoreDelta(1);
        positiveLog.setReason(GrowthScoreReason.LIKE_RECEIVED);
        positiveLog.setRefId(300L);
        positiveLog.setCreatedAt(LocalDateTime.now().minusSeconds(10));

        // 이미 음수 로그 1건 있음 (rollback 이미 됨)
        when(scoreLogRepository.findPositivesByUserReasonRef(1L, GrowthScoreReason.LIKE_RECEIVED, 300L))
            .thenReturn(List.of(positiveLog));
        when(scoreLogRepository.countNegativesByUserReasonRef(1L, GrowthScoreReason.LIKE_RECEIVED, 300L))
            .thenReturn(1);

        sut.onLikeRemoved(1L, 300L);

        // positives.size() <= negatives → no-op
        verify(scoreLogRepository, never()).save(any(GrowthScoreLog.class));
    }

    // ────────────────────────────────────────────────
    // 테스트 4: streak 7일 → 정확히 1회만 적립
    // ────────────────────────────────────────────────
    @Test
    void streak7_awarded_exactly_once() {
        // 첫 번째 호출 — 아직 없음
        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.ATTENDANCE_STREAK_7))
            .thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        sut.onAttendanceStreak(1L, 7, 10L);
        assertThat(growth.getGrowthScore()).isEqualTo(5);

        // 두 번째 호출 — 이미 받음
        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.ATTENDANCE_STREAK_7))
            .thenReturn(true);

        sut.onAttendanceStreak(1L, 7, 11L);
        assertThat(growth.getGrowthScore()).isEqualTo(5); // 증가 없음
    }

    // ────────────────────────────────────────────────
    // 테스트 5: 첫 게시글(30자 이상) → POST +2 + FIRST_POST +5 = 7점
    // ────────────────────────────────────────────────
    @Test
    void firstPost_over30chars_awards_7_points() {
        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.ONBOARDING_FIRST_POST))
            .thenReturn(false);
        when(scoreLogRepository.sumDailyByReason(
                eq(1L), eq(GrowthScoreReason.POST_CREATED), any(), any()))
            .thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        sut.onPostCreated(1L, 50L, 35); // 35자 게시글

        assertThat(growth.getGrowthScore()).isEqualTo(7);
    }

    // ────────────────────────────────────────────────
    // 테스트 6: 첫 게시글(29자 미만) → 0점
    // ────────────────────────────────────────────────
    @Test
    void firstPost_under30chars_awards_nothing() {
        sut.onPostCreated(1L, 50L, 29); // 29자 게시글

        verify(scoreLogRepository, never()).save(any(GrowthScoreLog.class));
        assertThat(growth.getGrowthScore()).isEqualTo(0);
    }

    // ────────────────────────────────────────────────
    // 테스트 7: stage 상승 → 알림 + outbox 1회
    // ────────────────────────────────────────────────
    @Test
    void stageUp_triggers_notification_and_outbox() {
        // score 19점 상태에서 좋아요 1개 받으면 20점 → 새싹(stage 1)으로 상승
        growth.setGrowthScore(19);
        growth.setStage(0);

        when(scoreLogRepository.existsByUserIdAndReason(1L, GrowthScoreReason.LIKE_RECEIVED))
            .thenReturn(false);
        when(scoreLogRepository.sumDailyByReason(
                eq(1L), eq(GrowthScoreReason.LIKE_RECEIVED), any(), any()))
            .thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        sut.onLikeReceived(1L, 400L);

        assertThat(growth.getStage()).isEqualTo(1);
        verify(notificationService, times(1))
            .createNotification(eq(user), eq(NotificationType.ANNOUNCEMENT),
                                anyString(), anyString(), isNull(), isNull());
        verify(outboundEventService, times(1)).publish(eq("STAGE_UP"), anyMap());
    }
}
