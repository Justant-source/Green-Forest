package com.vgc.service.event.photoexhibition;

import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoExhibitionPhaseResolverTest {
    private final PhotoExhibitionConfig config = config();

    @Test
    void submissionAndLockBoundaries() {
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-10T05:59:59")))
                .isEqualTo(PhotoExhibitionPhase.SCHEDULED);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-10T06:00:00")))
                .isEqualTo(PhotoExhibitionPhase.SUBMISSION);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-14T23:59:59")))
                .isEqualTo(PhotoExhibitionPhase.SUBMISSION);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-15T00:00:00")))
                .isEqualTo(PhotoExhibitionPhase.REVIEW);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-17T10:00:00")))
                .isEqualTo(PhotoExhibitionPhase.REVIEW);
    }

    @Test
    void votingStartsOnlyAfterAdminFlag() {
        config.setVotingStartedAt(at("2026-08-17T10:00:00"));
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-17T10:00:00")))
                .isEqualTo(PhotoExhibitionPhase.VOTING);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-20T23:59:59")))
                .isEqualTo(PhotoExhibitionPhase.VOTING);
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.ACTIVE, at("2026-08-21T00:00:00")))
                .isEqualTo(PhotoExhibitionPhase.TALLY_PENDING);
    }

    @Test
    void scored_event_is_result_regardless_of_clock() {
        assertThat(PhotoExhibitionPhaseResolver.resolve(config, EventStatus.SCORED, at("2026-08-10T06:00:00")))
                .isEqualTo(PhotoExhibitionPhase.RESULT);
    }

    private static PhotoExhibitionConfig config() {
        PhotoExhibitionConfig value = new PhotoExhibitionConfig();
        value.setSubmissionStart(at("2026-08-10T06:00:00"));
        value.setSubmissionEnd(at("2026-08-15T00:00:00"));
        value.setReviewEnd(at("2026-08-17T00:00:00"));
        value.setVotingEnd(at("2026-08-21T00:00:00"));
        return value;
    }

    private static LocalDateTime at(String value) { return LocalDateTime.parse(value); }
}
