package com.vgc.repository.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoExhibitionSubmissionRepository extends JpaRepository<PhotoExhibitionSubmission, Long> {
    Optional<PhotoExhibitionSubmission> findByEventIdAndUserId(Long eventId, Long userId);

    @EntityGraph(attributePaths = {"user", "images"})
    List<PhotoExhibitionSubmission> findByEventIdOrderByCreatedAtAsc(Long eventId);
}
