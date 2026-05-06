package com.vgc.repository;

import com.vgc.entity.BirthdayAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BirthdayAcknowledgementRepository extends JpaRepository<BirthdayAcknowledgement, Long> {
    boolean existsByAdminUserIdAndTargetUserIdAndBirthYear(Long adminUserId, Long targetUserId, int birthYear);
}
