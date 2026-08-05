package com.vgc.service.event;

import com.vgc.dto.event.EventModeResponse;
import com.vgc.dto.event.EventResponse;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.repository.event.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class EventModeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EventRepository eventRepository;
    private final EventService eventService;

    public EventModeService(EventRepository eventRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    public EventModeResponse getCurrentMode() {
        LocalDateTime now = LocalDateTime.now(KST);
        Optional<Event> active = eventRepository
                .findFirstByStatusOrderByEndAtAsc(EventStatus.ACTIVE);
        return active
                .map(e -> EventModeResponse.active(eventService.response(e), now))
                .orElse(EventModeResponse.none(now));
    }
}
