package com.vgc.repository;

import com.vgc.entity.Announcement;
import com.vgc.entity.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @Query("select a from Announcement a where a.active = true and (a.expiresAt is null or a.expiresAt > :now) order by a.createdAt desc")
    List<Announcement> findActiveVisible(@Param("now") LocalDateTime now);
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Announcement a SET a.active = false")
    void deactivateAll();

    @Modifying
    @Query("UPDATE Announcement a SET a.active = false WHERE a.type = :type")
    void deactivateAllByType(@Param("type") AnnouncementType type);
}
