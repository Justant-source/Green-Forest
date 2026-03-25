package com.vgc.repository;

import com.vgc.entity.DropTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vgc.entity.DropReasonType;
import java.time.LocalDateTime;
import java.util.List;

public interface DropTransactionRepository extends JpaRepository<DropTransaction, Long> {

    Page<DropTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM DropTransaction d WHERE d.user.id = :userId")
    int sumAmountByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM DropTransaction d " +
           "WHERE d.user.id = :userId AND d.createdAt >= :startDate AND d.createdAt < :endDate")
    int sumAmountByUserIdAndPeriod(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d.user.id, COALESCE(SUM(d.amount), 0) FROM DropTransaction d " +
           "WHERE d.createdAt >= :startDate AND d.createdAt < :endDate " +
           "GROUP BY d.user.id")
    List<Object[]> sumAmountGroupByUserForPeriod(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(d) FROM DropTransaction d " +
           "WHERE d.createdAt >= :startDate AND d.createdAt < :endDate")
    long countByPeriod(@Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM DropTransaction d " +
           "WHERE d.createdAt >= :startDate AND d.createdAt < :endDate AND d.amount > 0")
    int sumPositiveAmountByPeriod(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    boolean existsByUserIdAndReasonTypeAndRelatedPostId(Long userId, DropReasonType reasonType, Long relatedPostId);

    int countByUserIdAndReasonTypeAndRelatedPostId(Long userId, DropReasonType reasonType, Long relatedPostId);
}
