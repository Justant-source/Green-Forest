package com.vgc.dto.event;

import com.vgc.entity.event.photobingo.PhotoBingoConfig;

import java.time.LocalDateTime;

public class CreateEventRequest {

    private String type; // PHOTO_BINGO
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private PhotoBingoConfig config;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public PhotoBingoConfig getConfig() { return config; }
    public void setConfig(PhotoBingoConfig config) { this.config = config; }
}
