-- birth_date(LocalDate) → birth_month / birth_day, and registration_open setting
-- Dev only until prod is explicitly approved.

-- Idempotent-ish: run only if birth_date still exists.
-- If Hibernate already added birth_month/birth_day, skip ADD and just migrate + drop.

-- ALTER TABLE users
--   ADD COLUMN birth_month INT NULL AFTER earned_drops,
--   ADD COLUMN birth_day INT NULL AFTER birth_month;

UPDATE users
SET birth_month = MONTH(birth_date),
    birth_day = DAY(birth_date)
WHERE birth_date IS NOT NULL
  AND (birth_month IS NULL OR birth_day IS NULL);

ALTER TABLE users DROP COLUMN birth_date;

CREATE TABLE IF NOT EXISTS system_settings (
  setting_key VARCHAR(100) NOT NULL,
  setting_value VARCHAR(500) NOT NULL,
  PRIMARY KEY (setting_key)
);

INSERT INTO system_settings (setting_key, setting_value)
VALUES ('registration_open', 'true')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
