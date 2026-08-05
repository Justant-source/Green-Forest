package com.vgc.dto;

import com.vgc.entity.Announcement;
import java.time.LocalDateTime;

public class AnnouncementResponse {
    private Long id; private String title; private String content; private String type; private boolean active;
    private String relatedUrl; private String relatedLabel; private LocalDateTime expiresAt; private LocalDateTime createdAt;
    public static AnnouncementResponse from(Announcement a) { AnnouncementResponse r=new AnnouncementResponse(); r.id=a.getId(); r.title=a.getTitle(); r.content=a.getContent(); r.type=a.getType().name(); r.active=a.isActive(); r.relatedUrl=a.getRelatedEventUrl(); r.relatedLabel=a.getRelatedLabel(); r.expiresAt=a.getExpiresAt(); r.createdAt=a.getCreatedAt(); return r; }
    public Long getId(){return id;} public String getTitle(){return title;} public String getContent(){return content;} public String getType(){return type;} public boolean isActive(){return active;} public String getRelatedUrl(){return relatedUrl;} public String getRelatedLabel(){return relatedLabel;} public LocalDateTime getExpiresAt(){return expiresAt;} public LocalDateTime getCreatedAt(){return createdAt;}
}
