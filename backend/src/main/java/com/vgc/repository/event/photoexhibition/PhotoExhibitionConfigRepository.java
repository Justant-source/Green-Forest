package com.vgc.repository.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhotoExhibitionConfigRepository extends JpaRepository<PhotoExhibitionConfig, Long> {
    Optional<PhotoExhibitionConfig> findByEventId(Long eventId);
}
