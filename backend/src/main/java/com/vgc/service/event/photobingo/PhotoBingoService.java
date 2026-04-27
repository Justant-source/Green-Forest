package com.vgc.service.event.photobingo;

import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.EventType;
import com.vgc.entity.event.photobingo.CellScoreStatus;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.event.EventRepository;
import com.vgc.repository.event.photobingo.PhotoBingoCellRepository;
import com.vgc.repository.event.photobingo.PhotoBingoSubmissionRepository;
import com.vgc.service.ImageStorageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PhotoBingoService {

    private final EventRepository eventRepository;
    private final PhotoBingoSubmissionRepository submissionRepository;
    private final PhotoBingoCellRepository cellRepository;
    private final ImageStorageService imageStorageService;
    private final PhotoBingoPostSyncService postSyncService;
    private final PhotoBingoSubmissionCreator submissionCreator;

    public PhotoBingoService(EventRepository eventRepository,
                             PhotoBingoSubmissionRepository submissionRepository,
                             PhotoBingoCellRepository cellRepository,
                             ImageStorageService imageStorageService,
                             PhotoBingoPostSyncService postSyncService,
                             PhotoBingoSubmissionCreator submissionCreator) {
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
        this.cellRepository = cellRepository;
        this.imageStorageService = imageStorageService;
        this.postSyncService = postSyncService;
        this.submissionCreator = submissionCreator;
    }

    private Event requirePhotoBingoEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (event.getType() != EventType.PHOTO_BINGO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PHOTO_BINGO 타입 이벤트만 지원합니다.");
        }
        return event;
    }

    /**
     * 제출물을 조회하거나 없으면 생성한다.
     *
     * <p>동시 요청으로 인한 unique key 충돌(Duplicate entry)은 별도 REQUIRES_NEW 트랜잭션으로 격리된
     * {@link PhotoBingoSubmissionCreator}에서 처리되므로, 호출측 트랜잭션은 오염되지 않는다.</p>
     */
    public PhotoBingoSubmission getOrCreateSubmission(Event event, User user) {
        Optional<PhotoBingoSubmission> existing =
                submissionRepository.findByEventIdAndUserId(event.getId(), user.getId());
        if (existing.isPresent()) return existing.get();

        PhotoBingoConfig config = event.getConfigJson();
        if (config == null || config.getThemes() == null || config.getThemes().size() != 9) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 설정이 올바르지 않습니다.");
        }
        try {
            return submissionCreator.createAndFlush(event, user, config);
        } catch (DataIntegrityViolationException e) {
            // 바깥 TX 의 REPEATABLE_READ 스냅샷은 경쟁 TX 가 커밋한 행을 못 보므로,
            // REQUIRES_NEW 로 격리된 새 TX 에서 다시 조회해야 한다.
            return submissionCreator.findInNewTx(event.getId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "제출물 생성 실패", e));
        }
    }

    @Transactional(readOnly = true)
    public PhotoBingoSubmission getMySubmissionReadOnly(Long eventId, User user) {
        // read-only 경로에서는 이벤트 상태와 무관하게 조회만, 없으면 ENDED/SCORED에서는 조회만 가능한 빈 판을 얹지 않음
        Event event = requirePhotoBingoEvent(eventId);
        return submissionRepository.findByEventIdAndUserId(event.getId(), user.getId())
                .orElse(null);
    }

    /**
     * readOnly=true: 실제 INSERT 는 {@link PhotoBingoSubmissionCreator#createAndFlush}
     * (REQUIRES_NEW) 안에서 일어나므로 outer TX 는 쓰기가 없다. readOnly 로 지정해 auto-flush
     * 를 막아, 혹시 엔티티가 false dirty 로 판정되더라도 outer TX 가 UPDATE 를 내보내지
     * 않도록 한다. (과거 spurious UPDATE events 로 outer X 락 → inner INSERT self-block 버그 방지)
     */
    @Transactional(readOnly = true)
    public PhotoBingoSubmission getOrCreateMySubmission(Long eventId, User user) {
        Event event = requirePhotoBingoEvent(eventId);
        return getOrCreateSubmission(event, user);
    }

    @Transactional
    public PhotoBingoSubmission updateCaption(Long eventId, User user, String caption) {
        Event event = requirePhotoBingoEvent(eventId);
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이벤트가 진행 중이 아닙니다.");
        }
        PhotoBingoSubmission sub = getOrCreateSubmission(event, user);
        sub.setCaption(caption);
        postSyncService.syncPost(sub);
        return sub;
    }

    @Transactional
    public PhotoBingoSubmission uploadCellImage(Long eventId, int cellIndex, MultipartFile file, User user) {
        if (cellIndex < 0 || cellIndex > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cellIndex는 0~8이어야 합니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 없습니다.");
        }
        Event event = requirePhotoBingoEvent(eventId);
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이벤트가 진행 중이 아닙니다.");
        }
        PhotoBingoSubmission sub = getOrCreateSubmission(event, user);
        PhotoBingoCell cell = cellRepository.findBySubmissionIdAndCellIndex(sub.getId(), cellIndex)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "셀을 찾을 수 없습니다."));

        String previousUrl = cell.getImageUrl();
        try {
            String url = imageStorageService.upload(file);
            cell.setImageUrl(url);
            cell.setUploadedAt(LocalDateTime.now());
            cell.setScoreStatus(CellScoreStatus.PENDING);
            cell.setScoreComment(null);
            cell.setScoredBy(null);
            cell.setScoredAt(null);
            cellRepository.save(cell);
            // 기존 이미지 삭제는 저장 성공 후
            if (previousUrl != null && !previousUrl.equals(url)) {
                try {
                    imageStorageService.delete(previousUrl);
                } catch (Exception ignore) {
                    /* 실패해도 업로드는 유지 */
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.", e);
        }
        PhotoBingoSubmission refreshed = submissionRepository.findById(sub.getId()).orElseThrow();
        postSyncService.syncPost(refreshed);
        return refreshed;
    }

    @Transactional
    public PhotoBingoSubmission deleteCellImage(Long eventId, int cellIndex, User user) {
        if (cellIndex < 0 || cellIndex > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cellIndex는 0~8이어야 합니다.");
        }
        Event event = requirePhotoBingoEvent(eventId);
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이벤트가 진행 중이 아닙니다.");
        }
        PhotoBingoSubmission sub = submissionRepository
                .findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출물이 없습니다."));
        PhotoBingoCell cell = cellRepository.findBySubmissionIdAndCellIndex(sub.getId(), cellIndex)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "셀을 찾을 수 없습니다."));
        String previousUrl = cell.getImageUrl();
        cell.setImageUrl(null);
        cell.setUploadedAt(null);
        cell.setScoreStatus(CellScoreStatus.PENDING);
        cell.setScoreComment(null);
        cell.setScoredBy(null);
        cell.setScoredAt(null);
        cellRepository.save(cell);
        if (previousUrl != null) {
            try {
                imageStorageService.delete(previousUrl);
            } catch (Exception ignore) { /* 파일 삭제 실패는 치명적이지 않음 */ }
        }
        PhotoBingoSubmission refreshed = submissionRepository.findById(sub.getId()).orElseThrow();
        postSyncService.syncPost(refreshed);
        return refreshed;
    }

    @Transactional(readOnly = true)
    public List<PhotoBingoSubmission> listAllSubmissions(Long eventId) {
        requirePhotoBingoEvent(eventId);
        return submissionRepository.findAllByEventIdOrderByCreatedAtAsc(eventId);
    }

    /**
     * 최근 업로드 활동 N개 (전체 참여자의 셀 업로드 이벤트 피드)
     */
    @Transactional(readOnly = true)
    public List<PhotoBingoCell> listRecentUploads(Long eventId, int limit) {
        requirePhotoBingoEvent(eventId);
        return cellRepository.findRecentUploadsByEvent(eventId, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public PhotoBingoSubmission getSubmissionById(Long eventId, Long submissionId) {
        requirePhotoBingoEvent(eventId);
        PhotoBingoSubmission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출물을 찾을 수 없습니다."));
        if (!sub.getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이벤트와 제출물이 일치하지 않습니다.");
        }
        return sub;
    }
}
