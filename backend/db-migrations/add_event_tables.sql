-- 2026-04-23: 이벤트(타임어택 활동) 테이블 도입 + enum 컬럼 VARCHAR 확장 + 이름 정정
--   prod 환경(ddl-auto=validate)에서 수동 적용. dev 는 이미 적용됨.
--   포함:
--     1) events / event_participations / photo_bingo_submissions / photo_bingo_cells 테이블 생성
--     2) posts.photo_bingo_submission_id 컬럼 추가
--     3) drop_transactions.reason_type / notifications.type → VARCHAR(50) (enum 확장 문제 해결)
--     4) enum 값 이름 정정: EVENT_QUEST(퀘스트 완수 보상) → QUEST_COMPLETION
--     5) 카테고리 '이벤트' 추가

-- ─── (3) enum 컬럼을 VARCHAR(50) 로 확장 ───
-- 기존 MySQL ENUM 컬럼은 값 추가 시 DDL 필요 + Hibernate ddl-auto=update 가 확장을 못 하는 문제가 있어 VARCHAR 로 전환.
ALTER TABLE drop_transactions MODIFY COLUMN reason_type VARCHAR(50) NOT NULL;
ALTER TABLE notifications     MODIFY COLUMN type        VARCHAR(50) NOT NULL;

-- ─── (4) enum 값 이름 정정 ───
-- 기존 EVENT_QUEST 값은 실제 용도가 "퀘스트 완수 시 지급"이었으므로 QUEST_COMPLETION 으로 이름을 바로잡는다.
UPDATE drop_transactions SET reason_type = 'QUEST_COMPLETION' WHERE reason_type = 'EVENT_QUEST';

-- ─── (1) 이벤트 테이블 ───
CREATE TABLE events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
  config_json TEXT,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_events_creator FOREIGN KEY (created_by) REFERENCES users(id),
  KEY idx_events_status_endat (status, end_at DESC),
  KEY idx_events_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_participations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  event_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  joined_at DATETIME NOT NULL,
  total_drops_awarded INT NOT NULL DEFAULT 0,
  awarded_at DATETIME NULL,
  UNIQUE KEY uk_ep_event_user (event_id, user_id),
  CONSTRAINT fk_ep_event FOREIGN KEY (event_id) REFERENCES events(id),
  CONSTRAINT fk_ep_user  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_bingo_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  event_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  caption VARCHAR(1000),
  final_reward_drops INT NOT NULL DEFAULT 0,
  achieved_lines INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_pbs_event_user (event_id, user_id),
  CONSTRAINT fk_pbs_event FOREIGN KEY (event_id) REFERENCES events(id),
  CONSTRAINT fk_pbs_user  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_bingo_cells (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  submission_id BIGINT NOT NULL,
  cell_index INT NOT NULL,
  theme VARCHAR(100) NOT NULL,
  image_url VARCHAR(500),
  uploaded_at DATETIME,
  score_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  score_comment VARCHAR(200),
  scored_by BIGINT,
  scored_at DATETIME,
  UNIQUE KEY uk_pbc_submission_cell (submission_id, cell_index),
  KEY idx_pbc_score_status (score_status),
  CONSTRAINT fk_pbc_submission FOREIGN KEY (submission_id) REFERENCES photo_bingo_submissions(id),
  CONSTRAINT fk_pbc_scorer     FOREIGN KEY (scored_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── (2) posts 에 FK 컬럼 추가 ───
ALTER TABLE posts
  ADD COLUMN photo_bingo_submission_id BIGINT NULL,
  ADD UNIQUE KEY idx_posts_photo_bingo_submission (photo_bingo_submission_id);

-- ─── (5) 새 카테고리 '이벤트' 추가 ───
INSERT INTO categories (name, label, has_status, color, created_at)
  SELECT '이벤트', '이벤트', FALSE, 'purple', NOW()
  FROM DUAL
  WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = '이벤트');
