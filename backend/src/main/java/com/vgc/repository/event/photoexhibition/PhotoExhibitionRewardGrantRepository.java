package com.vgc.repository.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionRewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhotoExhibitionRewardGrantRepository extends JpaRepository<PhotoExhibitionRewardGrant, Long> {
    boolean existsByEventIdAndUserIdAndGrantKind(Long eventId, Long userId, String grantKind);
    Optional<PhotoExhibitionRewardGrant> findByEventIdAndUserIdAndGrantKind(Long eventId, Long userId, String grantKind);
}
