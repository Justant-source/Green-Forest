-- Prod schema sync (DATA-PRESERVING). Do not run from this task.
-- notifications.type must accept REQUEST (Hibernate ENUM did not add the value).

ALTER TABLE notifications
  MODIFY type VARCHAR(32) NOT NULL;
