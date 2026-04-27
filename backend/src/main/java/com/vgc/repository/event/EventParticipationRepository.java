package com.vgc.repository.event;

import com.vgc.entity.event.EventParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    Optional<EventParticipation> findByEventIdAndUserId(Long eventId, Long userId);
}
