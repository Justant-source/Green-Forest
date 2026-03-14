package com.vgc.repository;

import com.vgc.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findByActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT q FROM Quest q WHERE q.active = true " +
           "AND q.startDate <= :today AND q.endDate >= :today " +
           "ORDER BY q.createdAt DESC")
    List<Quest> findActiveQuestsForDate(@Param("today") LocalDate today);

    @Query("SELECT q FROM Quest q WHERE q.active = true " +
           "AND q.startDate <= :today AND q.endDate >= :today " +
           "AND (q.targetType = '전체' OR q.targetPartyId = :partyId) " +
           "ORDER BY q.createdAt DESC")
    List<Quest> findActiveQuestsForUserParty(@Param("today") LocalDate today,
                                             @Param("partyId") Long partyId);

    List<Quest> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    @Query("SELECT q FROM Quest q WHERE q.active = true AND q.endDate = :date")
    List<Quest> findQuestsEndingOn(@Param("date") LocalDate date);
}
