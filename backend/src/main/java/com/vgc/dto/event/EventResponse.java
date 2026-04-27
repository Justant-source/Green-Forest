package com.vgc.dto.event;

import com.vgc.entity.event.Event;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;

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
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        EventResponse r = new EventResponse();
        r.id = event.getId();
        r.type = event.getType() != null ? event.getType().name() : null;
        r.title = event.getTitle();
        r.description = event.getDescription();
        r.startAt = event.getStartAt();
        r.endAt = event.getEndAt();
        r.status = event.getStatus() != null ? event.getStatus().name() : null;
        r.config = event.getConfigJson();
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
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
