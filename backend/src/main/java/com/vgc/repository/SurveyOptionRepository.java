package com.vgc.repository;

import com.vgc.entity.SurveyOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {
    List<SurveyOption> findBySurveyIdOrderByDisplayOrderAscIdAsc(Long surveyId);
    long countBySurveyId(Long surveyId);
    long countBySurveyIdAndAddedByUserId(Long surveyId, Long userId);
}
