-- surveys 테이블에 requires_shipping 컬럼 추가
-- 실행 환경: prod DB (vgc_db, port 3306)
-- 순서 2/3: surveys 다음 실행 (survey_deliveries FK 의존)

ALTER TABLE `surveys`
  ADD COLUMN `requires_shipping` BIT(1) NOT NULL DEFAULT b'0';
