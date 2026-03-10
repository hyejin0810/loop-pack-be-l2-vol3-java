-- ============================================================
-- products 테이블 인덱스 추가 스크립트
-- 대상 DB : loopers
--
-- [실행 방법]
--   docker exec -i docker-mysql-1 mysql -uapplication -papplication < scripts/add-indexes.sql
--
-- [설계 근거]
--   idx_products_brand_likes  : 브랜드 필터 + 좋아요 정렬 커버
--   idx_products_brand_price  : 브랜드 필터 + 가격 정렬 커버
--   idx_products_created_at   : 전체 최신순 정렬 커버
--   deleted_at 미포함 이유     : 100% IS NULL → 카디널리티 1 → 인덱스 효과 없음
-- ============================================================

USE loopers;

-- ------------------------------------------------------------
-- 인덱스 추가
-- ------------------------------------------------------------
CREATE INDEX idx_products_brand_likes ON products (brand_id, likes_count);
CREATE INDEX idx_products_brand_price ON products (brand_id, price);
CREATE INDEX idx_products_created_at  ON products (created_at);

-- ------------------------------------------------------------
-- 결과 확인
-- ------------------------------------------------------------
SHOW INDEX FROM products;
