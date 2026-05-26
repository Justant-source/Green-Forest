package com.vgc.repository;

import com.vgc.entity.SurveyDelivery;
import com.vgc.entity.SurveyDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyDeliveryRepository extends JpaRepository<SurveyDelivery, Long> {
    List<SurveyDelivery> findBySurveyIdAndDeliveryStatusOrderByCreatedAtDesc(Long surveyId, SurveyDeliveryStatus status);
    List<SurveyDelivery> findByDeliveryStatusOrderByCreatedAtDesc(SurveyDeliveryStatus status);
    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);
}
