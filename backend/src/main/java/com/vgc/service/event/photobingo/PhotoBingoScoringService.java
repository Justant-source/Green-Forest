package com.vgc.service.event.photobingo;

import com.vgc.entity.NotificationType;
import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.photobingo.CellScoreStatus;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.event.EventRepository;
import com.vgc.repository.event.photobingo.PhotoBingoCellRepository;
import com.vgc.repository.event.photobingo.PhotoBingoSubmissionRepository;
import com.vgc.service.DropService;
import com.vgc.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PhotoBingoScoringService {

    private static final Logger log = LoggerFactory.getLogger(PhotoBingoScoringService.class);

    /** 3x3 빙고판 8줄 조합. (가로 3 + 세로 3 + 대각선 2) */
    private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final EventRepository eventRepository;
    private final PhotoBingoSubmissionRepository submissionRepository;
    private final PhotoBingoCellRepository cellRepository;
    private final DropService dropService;
    private final NotificationService notificationService;
    private final PhotoBingoPostSyncService postSyncService;

    public PhotoBingoScoringService(EventRepository eventRepository,
                                    PhotoBingoSubmissionRepository submissionRepository,
                                    PhotoBingoCellRepository cellRepository,
                                    DropService dropService,
                                    NotificationService notificationService,
                                    PhotoBingoPostSyncService postSyncService) {
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
        this.cellRepository = cellRepository;
        this.dropService = dropService;
        this.notificationService = notificationService;
        this.postSyncService = postSyncService;
    }

    public int countCompletedLines(Set<Integer> indices) {
        int n = 0;
        for (int[] line : LINES) {
            if (indices.contains(line[0]) && indices.contains(line[1]) && indices.contains(line[2])) n++;
        }
        return n;
    }

    public int calculateReward(Set<Integer> approved, PhotoBingoConfig config) {
        if (config == null || config.getRewards() == null) return 0;
        if (approved.size() == 9) return config.getRewards().getBlackout();
        int lines = countCompletedLines(approved);
        if (lines >= 5) return config.getRewards().getLine5();
        if (lines >= 3) return config.getRewards().getLine3();
        return 0;
    }

    private String rewardLabel(Set<Integer> approved, int lines) {
        if (approved.size() == 9) return "블랙아웃";
        if (lines >= 5) return "5줄 달성";
        if (lines >= 3) return "3줄 달성";
        return "";
    }

    @Transactional
    public PhotoBingoCell scoreCell(Long eventId, Long cellId, String scoreStatusStr, String comment, User admin) {
        CellScoreStatus target;
        try {
            target = CellScoreStatus.valueOf(scoreStatusStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scoreStatus가 올바르지 않습니다.");
        }
        if (target == CellScoreStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PENDING으로는 되돌릴 수 없습니다.");
        }
        PhotoBingoCell cell = cellRepository.findById(cellId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "셀을 찾을 수 없습니다."));
        if (!cell.getSubmission().getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트와 셀이 일치하지 않습니다.");
        }
        if (cell.getImageUrl() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미지가 업로드되지 않은 셀입니다.");
        }
        cell.setScoreStatus(target);
        cell.setScoreComment(comment);
        cell.setScoredBy(admin);
        cell.setScoredAt(LocalDateTime.now());
        postSyncService.syncPost(cell.getSubmission());
        return cell;
    }

    @Transactional
    public Event finalizeEvent(Long eventId, User admin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (event.getStatus() != EventStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ENDED 상태에서만 finalize 할 수 있습니다.");
        }

        long pending = cellRepository
                .countBySubmissionEventIdAndScoreStatus(eventId, CellScoreStatus.PENDING);
        // 업로드된 셀 중 PENDING이 있으면 거부. 업로드 안 된 PENDING은 무시하기 위해 image_url IS NULL 제외.
        // 간단화를 위해 submission 루프에서 직접 체크
        List<PhotoBingoSubmission> submissions = submissionRepository
                .findAllByEventIdOrderByCreatedAtAsc(eventId);
        for (PhotoBingoSubmission sub : submissions) {
            for (PhotoBingoCell c : sub.getCells()) {
                if (c.getImageUrl() != null && c.getScoreStatus() == CellScoreStatus.PENDING) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "전수 채점이 완료되지 않았습니다. (submission=" + sub.getId() + ", cell=" + c.getCellIndex() + ")");
                }
            }
        }

        PhotoBingoConfig config = event.getConfigJson();
        for (PhotoBingoSubmission sub : submissions) {
            Set<Integer> approved = sub.getCells().stream()
                    .filter(c -> c.getScoreStatus() == CellScoreStatus.APPROVED)
                    .map(PhotoBingoCell::getCellIndex)
                    .collect(Collectors.toCollection(HashSet::new));
            int lines = countCompletedLines(approved);
            int reward = calculateReward(approved, config);
            sub.setAchievedLines(lines);
            sub.setFinalRewardDrops(reward);
            if (reward > 0) {
                String label = rewardLabel(approved, lines);
                String memo = event.getTitle() + (label.isEmpty() ? "" : " (" + label + ")");
                dropService.awardEventReward(sub.getUser(), reward, memo);
                notificationService.createNotification(sub.getUser(), NotificationType.EVENT_REWARD,
                        "이벤트 보상 지급", memo + " — 💧" + reward + " 물방울을 받았어요!",
                        null, null);
            } else {
                notificationService.createNotification(sub.getUser(), NotificationType.EVENT_REWARD,
                        "이벤트 결과", event.getTitle() + " 결과: " + lines + "줄 달성. 다음에 꼭 빙고!",
                        null, null);
            }
            // 최종 집계 결과(achievedLines/finalRewardDrops)를 Post 마커에도 반영
            postSyncService.syncPost(sub);
        }
        event.setStatus(EventStatus.SCORED);
        log.info("[Event FINALIZE] id={} admin={} scoredSubmissions={}",
                eventId, admin.getNickname(), submissions.size());
        return event;
    }
}
