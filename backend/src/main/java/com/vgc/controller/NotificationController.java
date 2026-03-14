package com.vgc.controller;

import com.vgc.entity.Notification;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationService.getNotifications(user.getId(), page, size)
                .map(this::notificationToMap);
    }

    @GetMapping("/unread-count")
    public Map<String, Integer> getUnreadCount(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Map.of("count", notificationService.getUnreadCount(user.getId()));
    }

    @PutMapping("/{id}/read")
    public Map<String, String> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationService.markAsRead(id, user.getId());
        return Map.of("status", "read");
    }

    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationService.markAllAsRead(user.getId());
        return Map.of("status", "all_read");
    }

    private Map<String, Object> notificationToMap(Notification n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("type", n.getType().name());
        map.put("typeLabel", n.getType().getLabel());
        map.put("title", n.getTitle());
        map.put("body", n.getBody());
        map.put("relatedPostId", n.getRelatedPostId());
        map.put("relatedQuestId", n.getRelatedQuestId());
        map.put("isRead", n.isRead());
        map.put("createdAt", n.getCreatedAt().toString());
        return map;
    }
}
