package com.vgc.controller.event;

import com.vgc.dto.event.EventModeResponse;
import com.vgc.dto.event.EventResponse;
import com.vgc.entity.event.EventStatus;
import com.vgc.service.event.EventModeService;
import com.vgc.service.event.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventModeService eventModeService;

    public EventController(EventService eventService,
                                EventModeService eventModeService) {
        this.eventService = eventService;
        this.eventModeService = eventModeService;
    }

    @GetMapping("/mode")
    public EventModeResponse getMode() {
        return eventModeService.getCurrentMode();
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) String status) {
        List<EventStatus> statuses;
        if (status == null || status.isBlank()) {
            statuses = Arrays.asList(EventStatus.SCHEDULED, EventStatus.ACTIVE,
                    EventStatus.ENDED, EventStatus.SCORED);
        } else {
            String[] parts = status.split(",");
            statuses = Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(EventStatus::valueOf)
                    .toList();
        }
        return eventService.listByStatuses(statuses).stream()
                .map(EventResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return EventResponse.from(eventService.get(id));
    }
}
