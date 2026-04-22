package com.vgc.repository;

import com.vgc.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findTopByUserIdOrderByWeekStartDesc(Long userId);

    List<WeeklyReport> findByUserIdOrderByWeekStartDesc(Long userId);

    boolean existsByUserIdAndWeekStart(Long userId, LocalDate weekStart);
}
