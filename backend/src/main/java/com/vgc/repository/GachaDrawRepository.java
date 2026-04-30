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

    @Query("SELECT d FROM GachaDraw d JOIN FETCH d.user WHERE d.winner = true AND d.deliveryStatus = :status ORDER BY d.createdAt ASC")
    List<GachaDraw> findWinnersWithUserByStatus(@Param("status") GachaDeliveryStatus status);

    @Query("SELECT COUNT(d), SUM(CASE WHEN d.winner = true THEN 1 ELSE 0 END), SUM(CASE WHEN d.winner = true THEN d.prizeCashValue ELSE 0 END) " +
           "FROM GachaDraw d WHERE d.createdAt BETWEEN :from AND :to")
    Object[] findStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<GachaDraw> findByWinnerTrueAndCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    long countByUserIdAndWinnerTrueAndDeliveryStatus(Long userId, GachaDeliveryStatus status);

    @Query(value = "SELECT d FROM GachaDraw d JOIN FETCH d.user u WHERE " +
                   "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
                   "(:from IS NULL OR d.createdAt >= :from) AND " +
                   "(:to IS NULL OR d.createdAt <= :to) AND " +
                   "(:prizeId IS NULL OR d.prize.id = :prizeId) AND " +
                   "(:winnerOnly = false OR d.winner = true)",
           countQuery = "SELECT COUNT(d) FROM GachaDraw d JOIN d.user u WHERE " +
                        "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
                        "(:from IS NULL OR d.createdAt >= :from) AND " +
                        "(:to IS NULL OR d.createdAt <= :to) AND " +
                        "(:prizeId IS NULL OR d.prize.id = :prizeId) AND " +
                        "(:winnerOnly = false OR d.winner = true)")
    Page<GachaDraw> findAdminDraws(
            @Param("nickname") String nickname,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("prizeId") Long prizeId,
            @Param("winnerOnly") boolean winnerOnly,
            Pageable pageable);

    @Query("SELECT COUNT(d) FROM GachaDraw d JOIN d.user u WHERE " +
           "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
           "(:from IS NULL OR d.createdAt >= :from) AND " +
           "(:to IS NULL OR d.createdAt <= :to) AND " +
           "(:prizeId IS NULL OR d.prize.id = :prizeId)")
    long countTotalForStats(@Param("nickname") String nickname,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("prizeId") Long prizeId);

    @Query("SELECT COUNT(d) FROM GachaDraw d JOIN d.user u WHERE d.winner = true AND " +
           "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
           "(:from IS NULL OR d.createdAt >= :from) AND " +
           "(:to IS NULL OR d.createdAt <= :to) AND " +
           "(:prizeId IS NULL OR d.prize.id = :prizeId)")
    long countWinsForStats(@Param("nickname") String nickname,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           @Param("prizeId") Long prizeId);
}
