package com.vgc.repository;

import com.vgc.entity.OutboundEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboundEventRepository extends JpaRepository<OutboundEvent, Long> {

    @Query("SELECT e FROM OutboundEvent e WHERE e.id > :sinceId ORDER BY e.id ASC")
    List<OutboundEvent> findSince(Long sinceId, Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
