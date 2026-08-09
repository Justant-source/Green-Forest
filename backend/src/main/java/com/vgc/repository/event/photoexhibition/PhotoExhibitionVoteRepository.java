package com.vgc.repository.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PhotoExhibitionVoteRepository extends JpaRepository<PhotoExhibitionVote, Long> {
    List<PhotoExhibitionVote> findByEventIdAndVoterId(Long eventId, Long voterId);
    List<PhotoExhibitionVote> findByEventId(Long eventId);
    long countByEventIdAndSubmissionId(Long eventId, Long submissionId);
    @Modifying void deleteByEventIdAndVoterId(Long eventId, Long voterId);
    @Modifying void deleteBySubmissionId(Long submissionId);
}
