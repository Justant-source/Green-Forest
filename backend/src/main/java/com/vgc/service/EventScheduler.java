package com.vgc.service;

import com.vgc.service.event.EventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class EventScheduler {

    private final EventService eventService;

    public EventScheduler(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void transitionStatuses() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        eventService.activateDueEvents(now);
        eventService.endDueEvents(now);
    }
}
