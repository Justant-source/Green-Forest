package com.vgc.controller;

import com.vgc.entity.OutboundEvent;
import com.vgc.repository.OutboundEventRepository;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Hidden
@RestController
@RequestMapping("/api/notify")
public class NotifyEventController {

    private final OutboundEventRepository repository;

    public NotifyEventController(OutboundEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events(
            @RequestParam(name = "since", defaultValue = "0") long sinceId,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {

        if (limit < 1) limit = 1;
        if (limit > 500) limit = 500;

        List<OutboundEvent> rows = repository.findSince(sinceId, PageRequest.of(0, limit));
        return rows.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("eventType", e.getEventType());
            m.put("payload", e.getPayload());
            m.put("createdAt", e.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());
    }
}
