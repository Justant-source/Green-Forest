package com.vgc.repository;

import com.vgc.entity.GrowthScoreLog;
import com.vgc.entity.GrowthScoreReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GrowthScoreLogRepository extends JpaRepository<GrowthScoreLog, Long> {

    @Query("SELECT COALESCE(SUM(g.scoreDelta), 0) FROM GrowthScoreLog g " +
           "WHERE g.user.id = :userId AND g.reason = :reason " +
           "AND g.createdAt >= :from AND g.createdAt < :to")
    int sumDailyByReason(@Param("userId") Long userId,
                         @Param("reason") GrowthScoreReason reason,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    boolean existsByUserIdAndReason(Long userId, GrowthScoreReason reason);

    @Query("SELECT g FROM GrowthScoreLog g " +
           "WHERE g.user.id = :userId AND g.reason = :reason AND g.refId = :refId " +
           "AND g.scoreDelta > 0 ORDER BY g.createdAt ASC")
    List<GrowthScoreLog> findPositivesByUserReasonRef(@Param("userId") Long userId,
                                                      @Param("reason") GrowthScoreReason reason,
                                                      @Param("refId") Long refId);

    @Query("SELECT COUNT(g) FROM GrowthScoreLog g " +
           "WHERE g.user.id = :userId AND g.reason = :reason AND g.refId = :refId " +
           "AND g.scoreDelta < 0")
    int countNegativesByUserReasonRef(@Param("userId") Long userId,
                                      @Param("reason") GrowthScoreReason reason,
                                      @Param("refId") Long refId);
}
