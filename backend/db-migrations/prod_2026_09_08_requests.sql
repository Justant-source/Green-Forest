-- Prod schema sync (DATA-PRESERVING). Apply before backend restart (ddl-auto=validate).
-- Request tab: snack catalog, snack/supply requests, catalog sync, notification deep-link.
-- Matches Hibernate schema from SnackCatalogItem / SnackRequest / SupplyRequest / CatalogSyncRun.

CREATE TABLE snack_catalog_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ssg_item_id VARCHAR(40) NOT NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'SNACK',
  name VARCHAR(200) NOT NULL,
  image_url VARCHAR(500) NULL,
  ssg_url VARCHAR(500) NOT NULL,
  ssg_image_url VARCHAR(500) NULL,
  display_order INT NOT NULL,
  available BIT(1) NOT NULL,
  hidden_from_users BIT(1) NOT NULL,
  hidden_locked_by_admin BIT(1) NOT NULL,
  last_seen_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_snack_cat_ssg_category (ssg_item_id, category),
  KEY idx_snack_cat_avail_hidden_order (available, hidden_from_users, display_order),
  KEY idx_snack_cat_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE snack_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status ENUM('PENDING','PURCHASED','REJECTED') NOT NULL,
  reject_reason ENUM('OUT_OF_STOCK','POLICY','DUPLICATE','OTHER') NULL,
  reject_note VARCHAR(100) NULL,
  pending_owner_id BIGINT NULL,
  purchased_at DATETIME(6) NULL,
  rejected_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_snack_req_pending (catalog_item_id, pending_owner_id),
  KEY idx_snack_req_item_status (catalog_item_id, status),
  KEY idx_snack_req_user_created (user_id, created_at),
  CONSTRAINT fk_snack_req_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_snack_req_catalog FOREIGN KEY (catalog_item_id) REFERENCES snack_catalog_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supply_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(80) NOT NULL,
  quantity INT NOT NULL,
  memo VARCHAR(300) NULL,
  status ENUM('PENDING','PURCHASED','REJECTED') NOT NULL,
  reject_reason ENUM('OUT_OF_STOCK','POLICY','DUPLICATE','OTHER') NULL,
  reject_note VARCHAR(100) NULL,
  purchased_at DATETIME(6) NULL,
  rejected_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_supply_req_status_created (status, created_at),
  KEY idx_supply_req_user (user_id, created_at),
  CONSTRAINT fk_supply_req_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE catalog_sync_runs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status ENUM('RUNNING','SUCCESS','FAIL') NOT NULL,
  started_at DATETIME(6) NOT NULL,
  finished_at DATETIME(6) NULL,
  item_count INT NULL,
  error_message VARCHAR(500) NULL,
  started_by BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_catalog_sync_started_by FOREIGN KEY (started_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE notifications
  ADD COLUMN related_request_kind ENUM('SNACK','SUPPLY') NULL;
