-- survey_deliveries 테이블 생성 (dev SHOW CREATE TABLE 추출본)
-- 실행 환경: prod DB (vgc_db, port 3306)
-- 순서 3/3: users, surveys 컬럼 추가 완료 후 실행

CREATE TABLE `survey_deliveries` (
  `id`                     bigint NOT NULL AUTO_INCREMENT,
  `created_at`             datetime(6) NOT NULL,
  `delivered_at`           datetime(6) DEFAULT NULL,
  `delivered_by`           bigint DEFAULT NULL,
  `delivery_memo`          varchar(500) DEFAULT NULL,
  `delivery_status`        enum('PENDING','SHIPPED','DELIVERED','CANCELED') NOT NULL,
  `recipient_address_detail` varchar(200) DEFAULT NULL,
  `recipient_address_main` varchar(200) NOT NULL,
  `recipient_name`         varchar(50) NOT NULL,
  `recipient_phone`        varchar(20) NOT NULL,
  `recipient_zipcode`      varchar(10) NOT NULL,
  `tracking_number`        varchar(50) DEFAULT NULL,
  `option_id`              bigint NOT NULL,
  `survey_id`              bigint NOT NULL,
  `user_id`                bigint NOT NULL,
  `vote_id`                bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_survey_deliveries_vote` (`vote_id`),
  KEY `idx_sd_survey_status` (`survey_id`, `delivery_status`),
  KEY `idx_sd_user` (`user_id`, `created_at`),
  KEY `fk_sd_option` (`option_id`),
  CONSTRAINT `fk_sd_survey`  FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`),
  CONSTRAINT `fk_sd_user`    FOREIGN KEY (`user_id`)   REFERENCES `users` (`id`),
  CONSTRAINT `fk_sd_vote`    FOREIGN KEY (`vote_id`)   REFERENCES `survey_votes` (`id`),
  CONSTRAINT `fk_sd_option`  FOREIGN KEY (`option_id`) REFERENCES `survey_options` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
