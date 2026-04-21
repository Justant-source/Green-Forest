package com.vgc.repository;

import com.vgc.entity.AttendancePhrase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendancePhraseRepository extends JpaRepository<AttendancePhrase, Long> {
    List<AttendancePhrase> findByActiveTrue();
    List<AttendancePhrase> findByActiveTrueAndCategoryIn(List<String> categories);
}
