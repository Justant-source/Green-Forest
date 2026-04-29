-- ============================================================
-- 설문(투표) 기능 추가
-- Hibernate 6.x: LocalDateTime → DATETIME(6), @Enumerated(STRING) → ENUM(...)
-- ============================================================

-- 1. categories 에 admin_only 플래그 추가
ALTER TABLE categories ADD COLUMN admin_only BIT(1) NOT NULL DEFAULT 0;

-- 2. 설문 본체 (post_id 와 1:1)
CREATE TABLE surveys (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id                     BIGINT      NOT NULL,
    closes_at                   DATETIME(6) NOT NULL,
    is_anonymous                BIT(1)      NOT NULL DEFAULT 0,
    allow_option_add_by_user    BIT(1)      NOT NULL DEFAULT 0,
    allow_multi_select          BIT(1)      NOT NULL DEFAULT 0,
    is_notice                   BIT(1)      NOT NULL DEFAULT 0,
    created_at                  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_surveys_post (post_id),
    INDEX idx_surveys_notice_closes (is_notice, closes_at),
    CONSTRAINT fk_surveys_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 설문 옵션
CREATE TABLE survey_options (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id        BIGINT      NOT NULL,
    option_type      ENUM('TEXT_ONLY','IMAGE_ONLY','TEXT_AND_IMAGE') NOT NULL,
    text_content     VARCHAR(100) NULL,
    image_url        TEXT         NULL,
    display_order    INT          NOT NULL DEFAULT 0,
    added_by_user_id BIGINT       NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_opt_survey_order (survey_id, display_order),
    INDEX idx_opt_added_by (survey_id, added_by_user_id),
    CONSTRAINT fk_opt_survey FOREIGN KEY (survey_id)        REFERENCES surveys(id) ON DELETE CASCADE,
    CONSTRAINT fk_opt_user   FOREIGN KEY (added_by_user_id) REFERENCES users(id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 투표 기록 (anonymous 여부와 무관하게 모두 기록)
CREATE TABLE survey_votes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id   BIGINT      NOT NULL,
    option_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_votes_user_option (user_id, option_id),
    INDEX idx_votes_survey (survey_id),
    INDEX idx_votes_option (option_id),
    CONSTRAINT fk_vote_survey FOREIGN KEY (survey_id) REFERENCES surveys(id)        ON DELETE CASCADE,
    CONSTRAINT fk_vote_option FOREIGN KEY (option_id) REFERENCES survey_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_user   FOREIGN KEY (user_id)   REFERENCES users(id)          ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 설문 카테고리 시드
INSERT INTO categories (name, label, color, has_status, admin_only)
SELECT 'survey', '설문', 'indigo', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'survey');
