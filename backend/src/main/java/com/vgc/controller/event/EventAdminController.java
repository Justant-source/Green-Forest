package com.vgc.controller.event;

import com.vgc.dto.event.ChangeStatusRequest;
import com.vgc.dto.event.CreateEventRequest;
import com.vgc.dto.event.ExtendEventRequest;
import com.vgc.dto.event.EventResponse;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.event.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/events")
public class EventAdminController {

    private final EventService eventService;
    private final UserRepository userRepository;

    public EventAdminController(EventService eventService, UserRepository userRepository) {
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    private User requireAdmin(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return user;
    }

    @PostMapping
    public EventResponse create(@RequestBody CreateEventRequest req, Authentication auth) {
        User admin = requireAdmin(auth);
        return EventResponse.from(eventService.createEvent(req, admin));
    }

    @PatchMapping("/{id}/status")
    public EventResponse changeStatus(@PathVariable Long id,
                                           @RequestBody ChangeStatusRequest req,
                                           Authentication auth) {
        User admin = requireAdmin(auth);
        return EventResponse.from(eventService.changeStatus(id, req.getStatus(), admin));
    }

    @PatchMapping("/{id}/extend")
    public EventResponse extend(@PathVariable Long id,
                                     @RequestBody ExtendEventRequest req,
                                     Authentication auth) {
        User admin = requireAdmin(auth);
        return EventResponse.from(eventService.extendEvent(id, req.getAdditionalMinutes(), admin));
    }
}
