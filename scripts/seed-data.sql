-- ============================================================
-- 상품 목록 조회 성능 개선을 위한 테스트 데이터 적재 스크립트
-- 대상 DB : loopers
--
-- [실행 전 필수 조건]
--   1. Docker 실행: docker compose -f docker/infra-compose.yml up -d
--   2. 앱 실행:     ./gradlew :apps:commerce-api:bootRun
--      → Hibernate ddl-auto: create 가 brands, products, likes 테이블을 생성함
--   3. 앱 종료 후 이 스크립트 실행:
--      docker exec -i docker-mysql-1 mysql -uapplication -papplication < scripts/seed-data.sql
--
-- [주의] 앱을 재시작하면 ddl-auto: create 로 인해 테이블이 초기화됩니다.
--        데이터를 유지하려면 jpa.yml 의 ddl-auto 를 validate 로 변경 후 재시작하세요.
-- ============================================================

USE loopers;

-- ------------------------------------------------------------
-- 1. 브랜드 20개 INSERT
-- ------------------------------------------------------------
INSERT INTO brands (name, description, created_at, updated_at) VALUES
    ('Nike',        'Just Do It',                                        NOW(), NOW()),
    ('Adidas',      'Impossible is Nothing',                             NOW(), NOW()),
    ('Puma',        'Forever Faster',                                    NOW(), NOW()),
    ('New Balance', 'Fearlessly Independent',                            NOW(), NOW()),
    ('Reebok',      'Be More Human',                                     NOW(), NOW()),
    ('Under Armour','The Only Way is Through',                           NOW(), NOW()),
    ('Converse',    'Shoes Are Boring. Wear Sneakers',                   NOW(), NOW()),
    ('Vans',        'Off the Wall',                                      NOW(), NOW()),
    ('FILA',        'For the love of sport',                             NOW(), NOW()),
    ('Asics',       'Sound Mind Sound Body',                             NOW(), NOW()),
    ('Saucony',     'Run Your World',                                    NOW(), NOW()),
    ('Brooks',      'Run Happy',                                         NOW(), NOW()),
    ('Mizuno',      'For the Love of Sport',                             NOW(), NOW()),
    ('Salomon',     'Time to Play',                                      NOW(), NOW()),
    ('Columbia',    'Tested Tough',                                      NOW(), NOW()),
    ('Patagonia',   'We are in Business to Save Our Home Planet',        NOW(), NOW()),
    ('North Face',  'Never Stop Exploring',                              NOW(), NOW()),
    ('Lululemon',   'Elevate the World from Mediocrity',                 NOW(), NOW()),
    ('Champion',    'It Takes a Little More',                            NOW(), NOW()),
    ('Lacoste',     'A Little Green Crocodile',                          NOW(), NOW());

-- ------------------------------------------------------------
-- 2. 상품 100,000개 INSERT (Recursive CTE - 빠른 방식)
-- brand_id   : 1~20 랜덤 분포
-- price      : 1,000 ~ 500,000 (1,000원 단위)
-- stock      : 0 ~ 1,000
-- likes_count: 0 ~ 10,000 (롱테일 분포 - 대부분 낮고 소수만 높음)
-- created_at : 최근 1년 내 랜덤 날짜
-- ------------------------------------------------------------
SET cte_max_recursion_depth = 100000;

INSERT INTO products
    (brand_id, name, price, stock, description, image_url,
     version, likes_count, created_at, updated_at, deleted_at)
WITH RECURSIVE nums AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 100000
)
SELECT
    FLOOR(1 + RAND() * 20),
    CONCAT('상품_', LPAD(n, 6, '0')),
    FLOOR(1 + RAND() * 500) * 1000,
    FLOOR(RAND() * 1001),
    CONCAT('상품 ', n, '번의 상세 설명입니다.'),
    CONCAT('https://cdn.example.com/products/', n, '.jpg'),
    0,
    FLOOR(POW(RAND(), 3) * 10001),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
    NULL
FROM nums;

-- ------------------------------------------------------------
-- 3. 결과 확인
-- ------------------------------------------------------------
SELECT
    COUNT(*)                        AS total_products,
    COUNT(DISTINCT brand_id)        AS brand_count,
    MIN(price)                      AS min_price,
    MAX(price)                      AS max_price,
    ROUND(AVG(likes_count), 1)      AS avg_likes,
    MAX(likes_count)                AS max_likes
FROM products;
