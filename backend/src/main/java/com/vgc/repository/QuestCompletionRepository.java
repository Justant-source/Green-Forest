package com.vgc.repository;

import com.vgc.entity.QuestCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestCompletionRepository extends JpaRepository<QuestCompletion, Long> {

    int countByQuestIdAndUserId(Long questId, Long userId);

    boolean existsByQuestIdAndUserId(Long questId, Long userId);

    List<QuestCompletion> findByQuestId(Long questId);

    List<QuestCompletion> findByUserId(Long userId);

    List<QuestCompletion> findByQuestIdAndUserId(Long questId, Long userId);

    void deleteByPostId(Long postId);
}
