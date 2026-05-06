package com.vgc.repository;

import com.vgc.entity.Announcement;
import com.vgc.entity.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Optional<Announcement> findTopByActiveTrueOrderByCreatedAtDesc();
    List<Announcement> findAllByActiveTrueOrderByCreatedAtDesc();
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Announcement a SET a.active = false")
    void deactivateAll();

    @Modifying
    @Query("UPDATE Announcement a SET a.active = false WHERE a.type = :type")
    void deactivateAllByType(@Param("type") AnnouncementType type);
}
