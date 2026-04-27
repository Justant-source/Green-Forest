package com.vgc.service.event.photobingo;

import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.photobingo.CellScoreStatus;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import com.vgc.repository.event.photobingo.PhotoBingoSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PhotoBingoSubmission 생성 전용 서비스.
 *
 * REQUIRES_NEW 로 격리된 별도 트랜잭션에서 insert 를 수행해야,
 * 동시성 충돌로 DataIntegrityViolationException 이 던져졌을 때 호출측 트랜잭션이 오염되지 않는다.
 *
 * Spring AOP 프록시는 자기 자신을 호출할 때는 적용되지 않으므로, 반드시 별도 @Service 로 분리한다.
 */
@Service
public class PhotoBingoSubmissionCreator {

    private final PhotoBingoSubmissionRepository submissionRepository;

    public PhotoBingoSubmissionCreator(PhotoBingoSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PhotoBingoSubmission createAndFlush(Event event, User user, PhotoBingoConfig config) {
        PhotoBingoSubmission sub = new PhotoBingoSubmission();
        sub.setEvent(event);
        sub.setUser(user);

        List<PhotoBingoCell> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            PhotoBingoCell cell = new PhotoBingoCell();
            cell.setSubmission(sub);
            cell.setCellIndex(i);
            cell.setTheme(config.getThemes().get(i));
            cell.setScoreStatus(CellScoreStatus.PENDING);
            cells.add(cell);
        }
        sub.setCells(cells);
        return submissionRepository.saveAndFlush(sub);
    }

    /**
     * REQUIRES_NEW 로 격리된 새 트랜잭션에서 재조회한다. 바깥 트랜잭션의 REPEATABLE_READ
     * 스냅샷에 갇혀 동시에 커밋된 경쟁 트랜잭션의 행을 못 보는 문제를 피하기 위한 경로.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<PhotoBingoSubmission> findInNewTx(Long eventId, Long userId) {
        return submissionRepository.findByEventIdAndUserId(eventId, userId);
    }
}
