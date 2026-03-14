package com.vgc.repository;

import com.vgc.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByQuestIdAndUserId(Long questId, Long userId);

    Optional<Vote> findByQuestIdAndUserId(Long questId, Long userId);

    List<Vote> findByQuestId(Long questId);

    @Query("SELECT v.votedForOption, COUNT(v) FROM Vote v " +
           "WHERE v.quest.id = :questId AND v.votedForOption IS NOT NULL " +
           "GROUP BY v.votedForOption ORDER BY COUNT(v) DESC")
    List<Object[]> countByQuestIdGroupByOption(@Param("questId") Long questId);

    @Query("SELECT v.votedForUser.id, COUNT(v) FROM Vote v " +
           "WHERE v.quest.id = :questId AND v.votedForUser IS NOT NULL " +
           "GROUP BY v.votedForUser.id ORDER BY COUNT(v) DESC")
    List<Object[]> countByQuestIdGroupByUser(@Param("questId") Long questId);
}
