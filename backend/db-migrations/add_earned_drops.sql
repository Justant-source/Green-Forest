-- 2026-04-21: earned_drops 컬럼 추가 + 기존 데이터 백필
ALTER TABLE users ADD COLUMN earned_drops INT NOT NULL DEFAULT 0;

UPDATE users u
SET earned_drops = COALESCE(
  (SELECT SUM(amount) FROM drop_transactions
   WHERE user_id = u.id AND amount > 0), 0
);
