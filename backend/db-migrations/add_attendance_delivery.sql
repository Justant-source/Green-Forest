-- 2026-04-22: attendance_checkins 에 보상 전달 추적 컬럼 4개 추가
--   가챠(GachaDraw) 의 delivery_status / delivered_at / delivered_by / delivery_memo 패턴을 그대로 미러링
--   기존 winner=true row 들은 PENDING 으로 backfill (이미 당첨됐으나 아직 수령 안 된 상태)
--   컬럼 type 은 ENUM — Hibernate MySQL Dialect 가 @Enumerated(EnumType.STRING) 을 ENUM 으로 생성하는 기존 패턴 (gacha_draws 와 동일)
ALTER TABLE attendance_checkins
    ADD COLUMN delivery_status ENUM('NONE','PENDING','DELIVERED') NOT NULL DEFAULT 'NONE',
    ADD COLUMN delivered_at    DATETIME(6) NULL,
    ADD COLUMN delivered_by    BIGINT      NULL,
    ADD COLUMN delivery_memo   VARCHAR(500) NULL;

UPDATE attendance_checkins
SET delivery_status = 'PENDING'
WHERE is_winner = TRUE AND delivery_status = 'NONE';

CREATE INDEX idx_attendance_winner_delivery ON attendance_checkins(is_winner, delivery_status);
