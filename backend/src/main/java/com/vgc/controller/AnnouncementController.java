package com.vgc.controller;

import com.vgc.entity.Announcement;
import com.vgc.dto.AnnouncementResponse;
import com.vgc.repository.AnnouncementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<AnnouncementResponse> getActive() {
        return announcementRepository.findActiveVisible(LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"))).stream().findFirst()
                .map(AnnouncementResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/active-list")
    public ResponseEntity<List<AnnouncementResponse>> getActiveList() {
        List<AnnouncementResponse> list = announcementRepository.findActiveVisible(LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"))).stream().map(AnnouncementResponse::from).toList();
        return ResponseEntity.ok(list);
    }
}
