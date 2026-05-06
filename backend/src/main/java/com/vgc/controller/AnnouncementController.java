package com.vgc.controller;

import com.vgc.entity.Announcement;
import com.vgc.repository.AnnouncementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/active-list")
    public ResponseEntity<List<Announcement>> getActiveList() {
        List<Announcement> list = announcementRepository.findAllByActiveTrueOrderByCreatedAtDesc();
        return ResponseEntity.ok(list);
    }
}
