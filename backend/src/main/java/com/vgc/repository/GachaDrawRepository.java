package com.vgc.repository;

import com.vgc.entity.GachaDraw;
import com.vgc.entity.GachaDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GachaDrawRepository extends JpaRepository<GachaDraw, Long> {
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
    Page<GachaDraw> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<GachaDraw> findTop20ByWinnerTrueOrderByCreatedAtDesc();
    Page<GachaDraw> findByWinnerTrueAndDeliveryStatusOrderByCreatedAtAsc(GachaDeliveryStatus status, Pageable pageable);

    @Query("SELECT COUNT(d), SUM(CASE WHEN d.winner = true THEN 1 ELSE 0 END), SUM(CASE WHEN d.winner = true THEN d.prizeCashValue ELSE 0 END) " +
           "FROM GachaDraw d WHERE d.createdAt BETWEEN :from AND :to")
    Object[] findStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<GachaDraw> findByWinnerTrueAndCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);
}
