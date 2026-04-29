package com.vgc.repository;

import com.vgc.entity.SurveyVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveyVoteRepository extends JpaRepository<SurveyVote, Long> {
    List<SurveyVote> findBySurveyIdAndUserId(Long surveyId, Long userId);
    List<SurveyVote> findBySurveyId(Long surveyId);
    boolean existsByUserIdAndOptionId(Long userId, Long optionId);
    long countBySurveyIdAndUserId(Long surveyId, Long userId);

    @Modifying
    @Query("DELETE FROM SurveyVote v WHERE v.user.id = :userId AND v.option.id = :optionId")
    void deleteByUserIdAndOptionId(@Param("userId") Long userId, @Param("optionId") Long optionId);

    @Modifying
    @Query("DELETE FROM SurveyVote v WHERE v.option.id = :optionId")
    void deleteByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT v.option.id, COUNT(v) FROM SurveyVote v WHERE v.survey.id = :surveyId GROUP BY v.option.id")
    List<Object[]> countBySurveyGroupByOption(@Param("surveyId") Long surveyId);
}
