-- users 테이블에 배송지(주소·전화) 컬럼 추가
-- 실행 환경: prod DB (vgc_db, port 3306)
-- 순서 1/3: users 먼저 실행 (survey_deliveries FK 의존)

ALTER TABLE `users`
  ADD COLUMN `zipcode`        VARCHAR(10)  NULL AFTER `birth_date`,
  ADD COLUMN `address_main`   VARCHAR(200) NULL AFTER `zipcode`,
  ADD COLUMN `address_detail` VARCHAR(200) NULL AFTER `address_main`,
  ADD COLUMN `phone`          VARCHAR(20)  NULL AFTER `address_detail`;
