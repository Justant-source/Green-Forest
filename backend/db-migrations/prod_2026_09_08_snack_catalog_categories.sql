-- Prod schema sync (DATA-PRESERVING). Skip on first install if prod_2026_09_08_requests.sql
-- already created snack_catalog_items.category and uk_snack_cat_ssg_category.
-- Use only when upgrading a table created without category.

ALTER TABLE snack_catalog_items
  ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'SNACK' AFTER ssg_item_id;

ALTER TABLE snack_catalog_items
  DROP INDEX uk_snack_cat_ssg_item;

ALTER TABLE snack_catalog_items
  ADD UNIQUE KEY uk_snack_cat_ssg_category (ssg_item_id, category);

ALTER TABLE snack_catalog_items
  ADD KEY idx_snack_cat_category (category);
