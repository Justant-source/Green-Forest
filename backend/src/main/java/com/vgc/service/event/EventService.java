package com.vgc.service.event;

import com.vgc.dto.event.CreateEventRequest;
import com.vgc.entity.User;
import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import com.vgc.entity.event.EventType;
import com.vgc.entity.event.photobingo.PhotoBingoConfig;
import com.vgc.repository.event.EventRepository;
import com.vgc.repository.event.photoexhibition.PhotoExhibitionConfigRepository;
import com.vgc.dto.event.EventResponse;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import com.vgc.service.event.photoexhibition.PhotoExhibitionPhaseResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final PhotoExhibitionConfigRepository exhibitionConfigRepository;

    public EventService(EventRepository eventRepository, PhotoExhibitionConfigRepository exhibitionConfigRepository) {
        this.eventRepository = eventRepository;
        this.exhibitionConfigRepository = exhibitionConfigRepository;
    }

    @Transactional
    public Event createEvent(CreateEventRequest req, User admin) {
        if (req.getType() == null || (!req.getType().equals("PHOTO_BINGO") && !req.getType().equals("PHOTO_EXHIBITION"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이벤트 타입입니다.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title은 필수입니다.");
        }
        if (req.getStartAt() == null || req.getEndAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startAt / endAt은 필수입니다.");
        }
        if (!req.getEndAt().isAfter(req.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt은 startAt 이후여야 합니다.");
        }
        PhotoBingoConfig config = req.getConfig();
        if ("PHOTO_BINGO".equals(req.getType())) {
            if (config == null || config.getThemes() == null || config.getThemes().size() != 9 || config.getRewards() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진빙고 설정이 올바르지 않습니다.");
            }
        } else if (req.getPhotoExhibitionConfig() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoExhibitionConfig는 필수입니다.");
        }

        Event event = new Event();
        event.setType(EventType.valueOf(req.getType()));
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setStartAt(req.getStartAt());
        event.setEndAt(req.getEndAt());
        event.setStatus(EventStatus.DRAFT);
        event.setConfigJson(config);
        event.setCreatedBy(admin);
        event = eventRepository.save(event);
        if (event.getType() == EventType.PHOTO_EXHIBITION) {
            var c = req.getPhotoExhibitionConfig();
            if (c.getSubmissionStart() == null || c.getSubmissionEnd() == null
                    || c.getReviewEnd() == null || c.getVotingEnd() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전시회 모든 단계 시각은 필수입니다.");
            }
            if (!c.getSubmissionStart().isBefore(c.getSubmissionEnd()) || !c.getSubmissionEnd().isBefore(c.getReviewEnd()) || !c.getReviewEnd().isBefore(c.getVotingEnd())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전시회 단계 시간이 올바르지 않습니다.");
            }
            c.setEvent(event);
            event.setStartAt(c.getSubmissionStart());
            event.setEndAt(c.getVotingEnd());
            exhibitionConfigRepository.save(c);
        }
        return event;
    }

    @Transactional(readOnly = true)
    public EventResponse response(Event event) {
        if (event.getType() != EventType.PHOTO_EXHIBITION) return EventResponse.from(event);
        PhotoExhibitionConfig config = exhibitionConfigRepository.findByEventId(event.getId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        return EventResponse.from(event, config,
                PhotoExhibitionPhaseResolver.resolve(config, event.getStatus(), now), now);
    }

    @Transactional
    public Event changeStatus(Long eventId, String newStatus, User admin) {
        EventStatus target;
        try {
            target = EventStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 상태: " + newStatus);
        }
        if (target == EventStatus.SCORED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SCORED는 finalize API로만 전환할 수 있습니다.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        event.setStatus(target);
        log.info("[Event STATUS] id={} admin={} → {}", eventId, admin.getNickname(), target);
        return event;
    }

    @Transactional
    public Event extendEvent(Long eventId, int minutes, User admin) {
        if (minutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "additionalMinutes는 1 이상이어야 합니다.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ACTIVE 상태에서만 연장할 수 있습니다.");
        }
        LocalDateTime newEnd = event.getEndAt().plusMinutes(minutes);
        event.setEndAt(newEnd);
        log.warn("[Event EXTEND] id={} admin={} +{}min newEnd={}",
                eventId, admin.getNickname(), minutes, newEnd);
        return event;
    }

    @Transactional
    public void activateDueEvents(LocalDateTime now) {
        List<Event> due = eventRepository
                .findByStatusAndStartAtLessThanEqual(EventStatus.SCHEDULED, now);
        for (Event e : due) {
            if (e.getEndAt().isAfter(now)) {
                e.setStatus(EventStatus.ACTIVE);
                log.info("[Event AUTO-ACTIVE] id={} title={}", e.getId(), e.getTitle());
            }
        }
    }

    @Transactional
    public void endDueEvents(LocalDateTime now) {
        List<Event> due = eventRepository
                .findByStatusAndEndAtLessThanEqual(EventStatus.ACTIVE, now);
        for (Event e : due) {
            e.setStatus(EventStatus.ENDED);
            log.info("[Event AUTO-END] id={} title={}", e.getId(), e.getTitle());
        }
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<Event> listByStatuses(List<EventStatus> statuses) {
        return eventRepository.findByStatusInOrderByEndAtDesc(statuses);
    }
}
