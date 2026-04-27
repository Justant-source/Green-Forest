package com.vgc.dto.event;

import java.time.LocalDateTime;

public class EventModeResponse {

    private String mode; // "ACTIVE" | "NONE"
    private EventResponse event;
    private LocalDateTime serverNow;

    public static EventModeResponse none(LocalDateTime serverNow) {
        EventModeResponse r = new EventModeResponse();
        r.mode = "NONE";
        r.serverNow = serverNow;
        return r;
    }

    public static EventModeResponse active(EventResponse event, LocalDateTime serverNow) {
        EventModeResponse r = new EventModeResponse();
        r.mode = "ACTIVE";
        r.event = event;
        r.serverNow = serverNow;
        return r;
    }

    public String getMode() { return mode; }
    public EventResponse getEvent() { return event; }
    public LocalDateTime getServerNow() { return serverNow; }
}
