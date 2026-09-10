-- survey_options 항목 설명 (일반 회원 투표)
-- prod: ddl-auto=validate 이므로 기동 전 수동 적용. DATETIME/ENUM 변경 없음.

ALTER TABLE `survey_options`
  ADD COLUMN `description` VARCHAR(300) NULL AFTER `text_content`;
