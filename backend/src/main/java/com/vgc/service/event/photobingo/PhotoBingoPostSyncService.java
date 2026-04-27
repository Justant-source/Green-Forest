package com.vgc.service.event.photobingo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgc.entity.Post;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PhotoBingoSubmission 의 상태를 광장 Post 에 반영한다.
 *
 * - 업로드된 셀이 1장이라도 있으면 Post 를 생성/갱신한다.
 * - 모든 셀이 비어있으면 기존 Post 를 삭제한다.
 * - Post.content 에 `<!--photo-bingo:{json}-->` 마커를 삽입하여 프론트엔드가 3x3 그리드로 렌더하도록 한다.
 * - 썸네일(Post.imageUrl)은 업로드된 셀 중 가장 작은 cellIndex 의 이미지 URL 을 사용한다.
 */
@Service
public class PhotoBingoPostSyncService {

    private static final Logger log = LoggerFactory.getLogger(PhotoBingoPostSyncService.class);
    private static final String MARKER_PREFIX = "<!--photo-bingo:";
    private static final String MARKER_SUFFIX = "-->";

    private final PostRepository postRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PhotoBingoPostSyncService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public void syncPost(PhotoBingoSubmission submission) {
        Event event = submission.getEvent();
        if (event == null || submission.getUser() == null) return;

        List<PhotoBingoCell> cells = submission.getCells().stream()
                .sorted(Comparator.comparingInt(PhotoBingoCell::getCellIndex))
                .toList();
        long uploaded = cells.stream().filter(c -> c.getImageUrl() != null).count();

        Optional<Post> existing = postRepository.findByPhotoBingoSubmissionId(submission.getId());

        if (uploaded == 0) {
            existing.ifPresent(p -> {
                postRepository.delete(p);
                log.info("[PhotoBingoPost DELETE] submissionId={} postId={}", submission.getId(), p.getId());
            });
            return;
        }

        Post post = existing.orElseGet(() -> {
            Post p = new Post();
            p.setCategory("이벤트");
            p.setAuthor(submission.getUser());
            p.setPhotoBingoSubmissionId(submission.getId());
            p.setAnonymous(false);
            return p;
        });

        String thumbnail = cells.stream()
                .filter(c -> c.getImageUrl() != null)
                .map(PhotoBingoCell::getImageUrl)
                .findFirst()
                .orElse(null);
        post.setImageUrl(thumbnail);
        post.setTitle(event.getTitle());
        post.setContent(buildContent(submission, cells));

        postRepository.save(post);
    }

    private String buildContent(PhotoBingoSubmission submission, List<PhotoBingoCell> cells) {
        String caption = submission.getCaption() == null ? "" : submission.getCaption().trim();
        String marker = MARKER_PREFIX + buildMarkerJson(submission, cells) + MARKER_SUFFIX;
        return caption.isBlank() ? marker : caption + "\n\n" + marker;
    }

    private String buildMarkerJson(PhotoBingoSubmission sub, List<PhotoBingoCell> cells) {
        Event event = sub.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("submissionId", sub.getId());
        payload.put("eventId", event.getId());
        payload.put("eventTitle", event.getTitle());
        payload.put("eventStatus", event.getStatus().name());
        payload.put("achievedLines", sub.getAchievedLines());
        payload.put("finalRewardDrops", sub.getFinalRewardDrops());

        List<Map<String, Object>> cellList = cells.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idx", c.getCellIndex());
            m.put("theme", c.getTheme());
            m.put("imageUrl", c.getImageUrl());
            m.put("scoreStatus", c.getScoreStatus() != null ? c.getScoreStatus().name() : "PENDING");
            return m;
        }).toList();
        payload.put("cells", cellList);

        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize photo-bingo marker for submission {}: {}", sub.getId(), e.getMessage());
            return "{}";
        }
    }
}
