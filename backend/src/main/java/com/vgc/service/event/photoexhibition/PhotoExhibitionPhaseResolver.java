package com.vgc.service.event.photoexhibition;

import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;

import java.time.LocalDateTime;

/** KST is supplied by callers; this pure resolver makes boundary rules testable. */
public final class PhotoExhibitionPhaseResolver {
    private PhotoExhibitionPhaseResolver() { }

    public static PhotoExhibitionPhase resolve(PhotoExhibitionConfig config, EventStatus status, LocalDateTime now) {
        if (status == EventStatus.SCORED) return PhotoExhibitionPhase.RESULT;
        if (now.isBefore(config.getSubmissionStart())) return PhotoExhibitionPhase.SCHEDULED;
        if (now.isBefore(config.getSubmissionEnd())) return PhotoExhibitionPhase.SUBMISSION;
        // After submission: stay locked until admin opens voting (votingStartedAt).
        if (config.getVotingStartedAt() == null) return PhotoExhibitionPhase.REVIEW;
        if (now.isBefore(config.getVotingEnd())) return PhotoExhibitionPhase.VOTING;
        return PhotoExhibitionPhase.TALLY_PENDING;
    }
}
