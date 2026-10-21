-- 목록 갱신에서 사라져 available=0 이 된 상품을 사용자에게 숨김 처리 (소급 적용)
-- 코드(SnackCatalogSyncService)가 앞으로는 자동으로 숨긴다. 이 SQL은 기존 데이터용.
UPDATE snack_catalog_items
SET hidden_from_users = 1
WHERE available = 0 AND hidden_from_users = 0;
