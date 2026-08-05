package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.vgc.entity.AnnouncementType;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type = AnnouncementType.MANUAL;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "related_event_url", length = 300)
    private String relatedEventUrl;

    @Column(name = "related_label", length = 80)
    private String relatedLabel;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public AnnouncementType getType() { return type; }
    public void setType(AnnouncementType type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedEventUrl() { return relatedEventUrl; }
    public void setRelatedEventUrl(String relatedEventUrl) { this.relatedEventUrl = relatedEventUrl; }
    public String getRelatedLabel() { return relatedLabel; }
    public void setRelatedLabel(String relatedLabel) { this.relatedLabel = relatedLabel; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
