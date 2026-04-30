-- ============================================================
-- growth_score_log 테이블 생성 + plant_growth 컬럼 추가
-- Hibernate 6.x 규칙: @Enumerated(STRING) → ENUM(...), LocalDateTime → DATETIME(6)
-- ============================================================
CREATE TABLE growth_score_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    score_delta  INT    NOT NULL,
    reason       ENUM(
        'ATTENDANCE_DAILY',
        'ATTENDANCE_STREAK_7',
        'ATTENDANCE_STREAK_30',
        'ATTENDANCE_STREAK_100',
        'ATTENDANCE_DRAW_WIN',
        'POST_CREATED',
        'LIKE_RECEIVED',
        'COMMENT_RECEIVED',
        'PRAISE_RECEIVED',
        'GACHA_WIN',
        'ONBOARDING_FIRST_POST',
        'ONBOARDING_FIRST_COMMENT',
        'ADMIN_ADJUSTMENT'
    ) NOT NULL,
    ref_id       BIGINT NULL,
    created_at   DATETIME(6) NOT NULL,
    INDEX idx_gsl_user_date      (user_id, created_at),
    INDEX idx_gsl_user_reason    (user_id, reason),
    INDEX idx_gsl_user_reason_ref (user_id, reason, ref_id),
    CONSTRAINT fk_gsl_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE plant_growth
    ADD COLUMN last_stage_up_score INT NOT NULL DEFAULT 0,
    ADD COLUMN attendance_score    INT NOT NULL DEFAULT 0,
    ADD COLUMN post_score          INT NOT NULL DEFAULT 0,
    ADD COLUMN gacha_score         INT NOT NULL DEFAULT 0,
    ADD COLUMN onboarding_score    INT NOT NULL DEFAULT 0;

-- 기존 사용자 stage 새 임계치(0/20/80/200/400/700)로 재계산 (stage 다운 허용)
UPDATE plant_growth SET stage = CASE
    WHEN growth_score >= 700 THEN 5
    WHEN growth_score >= 400 THEN 4
    WHEN growth_score >= 200 THEN 3
    WHEN growth_score >= 80  THEN 2
    WHEN growth_score >= 20  THEN 1
    ELSE 0
END;
