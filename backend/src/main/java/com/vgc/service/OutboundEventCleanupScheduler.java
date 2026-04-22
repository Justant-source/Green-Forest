package com.vgc.service;

import com.vgc.repository.OutboundEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class OutboundEventCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboundEventCleanupScheduler.class);
    private final OutboundEventRepository repository;

    public OutboundEventCleanupScheduler(OutboundEventRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(90);
        long n = repository.deleteByCreatedAtBefore(cutoff);
        log.info("OutboundEvent cleanup: {} rows deleted (older than {})", n, cutoff);
    }
}
