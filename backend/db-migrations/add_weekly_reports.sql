-- 2026-04-21: weekly_reports 테이블 추가
CREATE TABLE IF NOT EXISTS weekly_reports (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  week_start DATE NOT NULL,
  week_end DATE NOT NULL,
  earned_amount INT NOT NULL DEFAULT 0,
  party_rank INT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_week (user_id, week_start),
  CONSTRAINT fk_weekly_reports_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_weekly_reports_user (user_id),
  KEY idx_weekly_reports_week_start (week_start),
  KEY idx_weekly_reports_user_week_start (user_id, week_start DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
