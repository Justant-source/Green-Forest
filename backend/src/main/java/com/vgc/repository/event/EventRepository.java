package com.vgc.repository.event;

import com.vgc.entity.event.Event;
import com.vgc.entity.event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusInOrderByEndAtDesc(List<EventStatus> statuses);

    List<Event> findByStatusAndStartAtLessThanEqual(EventStatus status, LocalDateTime cutoff);

    List<Event> findByStatusAndEndAtLessThanEqual(EventStatus status, LocalDateTime cutoff);

    Optional<Event> findFirstByStatusOrderByEndAtAsc(EventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
