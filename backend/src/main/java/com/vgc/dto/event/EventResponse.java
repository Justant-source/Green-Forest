package com.vgc.dto.event;

import com.vgc.entity.event.Event;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import com.vgc.dto.event.photoexhibition.PhotoExhibitionConfigResponse;
import com.vgc.service.event.photoexhibition.PhotoExhibitionPhase;

import java.time.LocalDateTime;

public class EventResponse {

    private Long id;
    private String type;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private PhotoBingoConfig config;
    private PhotoExhibitionConfigResponse photoExhibitionConfig;
    private String phase;
    private LocalDateTime serverNow;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return from(event, null, null, null);
    }

    public static EventResponse from(Event event, PhotoExhibitionConfig exhibitionConfig,
                                     PhotoExhibitionPhase phase, LocalDateTime serverNow) {
        EventResponse r = new EventResponse();
        r.id = event.getId();
        r.type = event.getType() != null ? event.getType().name() : null;
        r.title = event.getTitle();
        r.description = event.getDescription();
        r.startAt = event.getStartAt();
        r.endAt = event.getEndAt();
        r.status = event.getStatus() != null ? event.getStatus().name() : null;
        r.config = event.getConfigJson();
        if (exhibitionConfig != null) r.photoExhibitionConfig = PhotoExhibitionConfigResponse.from(exhibitionConfig);
        r.phase = phase == null ? null : phase.name();
        r.serverNow = serverNow;
        r.createdBy = event.getCreatedBy() != null ? event.getCreatedBy().getId() : null;
        r.createdAt = event.getCreatedAt();
        r.updatedAt = event.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public String getStatus() { return status; }
    public PhotoBingoConfig getConfig() { return config; }
    public PhotoExhibitionConfigResponse getPhotoExhibitionConfig() { return photoExhibitionConfig; }
    public String getPhase() { return phase; }
    public LocalDateTime getServerNow() { return serverNow; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
