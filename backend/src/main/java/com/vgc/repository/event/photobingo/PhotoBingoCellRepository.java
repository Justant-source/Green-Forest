package com.vgc.repository.event.photobingo;

import com.vgc.entity.event.photobingo.CellScoreStatus;
import com.vgc.entity.event.photobingo.PhotoBingoCell;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoBingoCellRepository extends JpaRepository<PhotoBingoCell, Long> {

    Optional<PhotoBingoCell> findBySubmissionIdAndCellIndex(Long submissionId, int cellIndex);

    List<PhotoBingoCell> findAllBySubmissionIdOrderByCellIndexAsc(Long submissionId);

    long countBySubmissionEventIdAndScoreStatus(Long eventId, CellScoreStatus scoreStatus);

    @Query("""
        SELECT c FROM PhotoBingoCell c
        JOIN FETCH c.submission s
        JOIN FETCH s.user u
        WHERE s.event.id = :eventId
          AND c.imageUrl IS NOT NULL
          AND c.uploadedAt IS NOT NULL
        ORDER BY c.uploadedAt DESC
    """)
    List<PhotoBingoCell> findRecentUploadsByEvent(@Param("eventId") Long eventId, Pageable pageable);
}
