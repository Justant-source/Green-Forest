package com.vgc.entity.event;

import com.vgc.entity.User;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_status_endat", columnList = "status, end_at DESC"),
    @Index(name = "idx_events_type", columnList = "type")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus status = EventStatus.DRAFT;

    @Convert(converter = PhotoBingoConfigConverter.class)
    @Column(name = "config_json", columnDefinition = "TEXT")
    private PhotoBingoConfig configJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public PhotoBingoConfig getConfigJson() { return configJson; }
    public void setConfigJson(PhotoBingoConfig configJson) { this.configJson = configJson; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
