-- Prod schema sync (DATA-PRESERVING)
-- - birth_date → birth_month/birth_day (기존 생일 값 복사 후 컬럼만 제거)
-- - system_settings / photo exhibition 신규 테이블·컬럼 추가
-- - 기존 users/posts/events 행은 DELETE 하지 않음

-- 1) 생일: 월/일로 변환 (데이터 유지)
ALTER TABLE users
  ADD COLUMN birth_month INT NULL AFTER birth_date,
  ADD COLUMN birth_day INT NULL AFTER birth_month;

UPDATE users
SET birth_month = MONTH(birth_date),
    birth_day = DAY(birth_date)
WHERE birth_date IS NOT NULL;

ALTER TABLE users DROP COLUMN birth_date;

-- 2) 가입 허용 설정
CREATE TABLE IF NOT EXISTS system_settings (
  setting_key VARCHAR(100) NOT NULL,
  setting_value VARCHAR(500) NOT NULL,
  PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, setting_value)
VALUES ('registration_open', 'true')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

-- 3) 사진전 / 공지 enum·컬럼
ALTER TABLE events MODIFY type ENUM('PHOTO_BINGO','PHOTO_EXHIBITION') NOT NULL;
ALTER TABLE announcements MODIFY type ENUM('MANUAL','BIRTHDAY','EVENT') NOT NULL;
ALTER TABLE announcements ADD COLUMN related_event_url VARCHAR(300) NULL;
ALTER TABLE announcements ADD COLUMN related_label VARCHAR(80) NULL;
ALTER TABLE announcements ADD COLUMN expires_at DATETIME(6) NULL;
ALTER TABLE posts ADD COLUMN photo_exhibition_submission_id BIGINT NULL;
ALTER TABLE posts ADD UNIQUE KEY idx_posts_photo_exhibition_submission (photo_exhibition_submission_id);
ALTER TABLE posts ADD COLUMN photo_exhibition_event_id BIGINT NULL;
ALTER TABLE posts ADD KEY idx_posts_photo_exhibition_event (photo_exhibition_event_id);

-- 4) 사진전 테이블 (비어 있는 신규 테이블)
CREATE TABLE photo_exhibition_configs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  review_end DATETIME(6) NOT NULL,
  submission_end DATETIME(6) NOT NULL,
  submission_start DATETIME(6) NOT NULL,
  voting_end DATETIME(6) NOT NULL,
  event_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_5ubd4nr5odbvn8h1mm4tgq8qf (event_id),
  CONSTRAINT FKn4q30img0ackko2ffqvhisjd FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_exhibition_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  excluded BIT(1) NOT NULL,
  exclusion_reason VARCHAR(500) NULL,
  final_votes INT NOT NULL,
  introduction TEXT NULL,
  plaza_post_id BIGINT NULL,
  result_tier VARCHAR(20) NULL,
  title VARCHAR(150) NOT NULL,
  event_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_photo_exhibition_event_user (event_id, user_id),
  KEY FK15d6bi829p96hlfh6nn8e4o1d (user_id),
  KEY idx_exhibition_submission_event (event_id),
  CONSTRAINT FK15d6bi829p96hlfh6nn8e4o1d FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT FK7t5gm4a1b4p3if6p9xlpjn4w9 FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_exhibition_images (
  id BIGINT NOT NULL AUTO_INCREMENT,
  image_url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL,
  submission_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exhibition_image_order (submission_id, sort_order),
  CONSTRAINT FKorx0tdt6or9g1ov2816i59ujf FOREIGN KEY (submission_id) REFERENCES photo_exhibition_submissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_exhibition_votes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL,
  event_id BIGINT NOT NULL,
  submission_id BIGINT NOT NULL,
  voter_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exhibition_voter_work (event_id, voter_id, submission_id),
  KEY idx_exhibition_vote_event (event_id),
  KEY idx_exhibition_vote_submission (submission_id),
  KEY idx_exhibition_vote_voter (voter_id),
  CONSTRAINT FKegbl0rajrpib7fybv3jb6w1mo FOREIGN KEY (voter_id) REFERENCES users (id),
  CONSTRAINT FKjuqjvaixc6e08vk3khojok1el FOREIGN KEY (submission_id) REFERENCES photo_exhibition_submissions (id),
  CONSTRAINT FKknuohucilajhqv9xl1exp8b4k FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE photo_exhibition_reward_grants (
  id BIGINT NOT NULL AUTO_INCREMENT,
  amount INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  grant_kind VARCHAR(30) NOT NULL,
  event_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exhibition_reward_once (event_id, user_id, grant_kind),
  KEY FK21qilnlm1n4rvyjjj8p1vobb8 (user_id),
  CONSTRAINT FK21qilnlm1n4rvyjjj8p1vobb8 FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT FKse2evg2ei2sdm6oyegdj7bqfr FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
