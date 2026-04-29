package com.vgc.repository;

import com.vgc.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Optional<Survey> findByPostId(Long postId);

    @Query("SELECT s FROM Survey s JOIN FETCH s.post WHERE s.notice = true AND s.closesAt > :now ORDER BY s.createdAt DESC")
    List<Survey> findActiveNotices(@Param("now") LocalDateTime now);
}
