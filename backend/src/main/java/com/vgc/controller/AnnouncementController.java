package com.vgc.controller;

import com.vgc.entity.Announcement;
import com.vgc.repository.AnnouncementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<Announcement> getActive() {
        return announcementRepository.findTopByActiveTrueOrderByCreatedAtDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
