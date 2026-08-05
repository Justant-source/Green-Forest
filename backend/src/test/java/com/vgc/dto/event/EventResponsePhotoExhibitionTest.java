package com.vgc.dto.event;

import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.EventType;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import com.vgc.service.event.photoexhibition.PhotoExhibitionPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponsePhotoExhibitionTest {
    @Test
    void photoExhibitionResponse_exposesConfigPhaseAndServerClock() {
        Event event = new Event(); event.setType(EventType.PHOTO_EXHIBITION); event.setStatus(EventStatus.ACTIVE);
        PhotoExhibitionConfig config = new PhotoExhibitionConfig(); config.setSubmissionStart(LocalDateTime.parse("2026-08-10T06:00:00")); config.setSubmissionEnd(LocalDateTime.parse("2026-08-15T06:00:00")); config.setReviewEnd(LocalDateTime.parse("2026-08-17T06:00:00")); config.setVotingEnd(LocalDateTime.parse("2026-08-20T06:00:00"));
        LocalDateTime now = LocalDateTime.parse("2026-08-17T06:00:00");
        EventResponse response = EventResponse.from(event, config, PhotoExhibitionPhase.VOTING, now);
        assertThat(response.getPhase()).isEqualTo("VOTING"); assertThat(response.getServerNow()).isEqualTo(now); assertThat(response.getPhotoExhibitionConfig().getVotingEnd()).isEqualTo(config.getVotingEnd());
    }
}
