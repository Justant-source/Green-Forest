package com.vgc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgc.entity.OutboundEvent;
import com.vgc.repository.OutboundEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OutboundEventService {

    private static final Logger log = LoggerFactory.getLogger(OutboundEventService.class);

    private final OutboundEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboundEventService(OutboundEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // 호출자의 트랜잭션에 join — winner 저장과 같은 커밋에 묶어 이벤트 유실/가짜 알림 방지.
    // REQUIRES_NEW 로 바꾸면 winner 롤백 시에도 이벤트가 발송되므로 금지.
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(String eventType, Map<String, Object> payload) {
        try {
            OutboundEvent e = new OutboundEvent();
            e.setEventType(eventType);
            e.setPayload(objectMapper.writeValueAsString(payload));
            repository.save(e);
        } catch (JsonProcessingException ex) {
            log.error("OutboundEvent payload JSON 변환 실패: type={}, payload={}", eventType, payload, ex);
        } catch (Exception ex) {
            log.error("OutboundEvent publish 실패: type={}", eventType, ex);
        }
    }
}
