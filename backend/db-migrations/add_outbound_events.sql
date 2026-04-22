-- 2026-04-22: outbound_events 테이블 추가 (사내 우분투 polling 용 Outbox)
CREATE TABLE outbound_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL,
    payload     JSON         NOT NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_outbound_events_id_type (id, event_type),
    INDEX idx_outbound_events_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
