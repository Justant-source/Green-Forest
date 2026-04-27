package com.vgc.repository.event.photobingo;

import com.vgc.entity.event.photobingo.PhotoBingoSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoBingoSubmissionRepository extends JpaRepository<PhotoBingoSubmission, Long> {

    Optional<PhotoBingoSubmission> findByEventIdAndUserId(Long eventId, Long userId);

    List<PhotoBingoSubmission> findAllByEventIdOrderByCreatedAtAsc(Long eventId);
}
