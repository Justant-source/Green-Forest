package com.vgc.repository;

import com.vgc.entity.QuestCompletionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestCompletionLogRepository extends JpaRepository<QuestCompletionLog, Long> {

    Optional<QuestCompletionLog> findByUserIdAndQuestTypeAndCategoryAndPeriodKey(
        Long userId, String questType, String category, String periodKey
    );

    boolean existsByUserIdAndQuestTypeAndCategoryAndPeriodKey(
        Long userId, String questType, String category, String periodKey
    );
}
