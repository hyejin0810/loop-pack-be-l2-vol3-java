erDiagram
%% ========================================
%% 사용자
%% ========================================
USERS {
bigint id PK "자동증가"
varchar login_id UK "로그인 ID"
varchar password "BCrypt 해시"
varchar name "사용자 이름"
varchar email "이메일"
varchar phone "연락처"
datetime created_at "가입일시"
datetime updated_at "수정일시"
}

%% ========================================
%% 브랜드
%% ========================================
BRANDS {
bigint id PK "자동증가"
varchar name "브랜드명"
text description "브랜드 설명"
datetime created_at "생성일시"
datetime updated_at "수정일시"
datetime deleted_at "삭제일시"
}

%% ========================================
%% 상품
%% ========================================
PRODUCTS {
bigint id PK "자동증가"
bigint brand_id FK "브랜드 ID"
varchar name "상품명"
int price "가격"
int stock "재고"
text description "상품 설명"
varchar image_url "상품 이미지"
int version "낙관적 락"
int likes_count "좋아요 수"
datetime created_at "등록일시"
datetime updated_at "수정일시"
datetime deleted_at "삭제일시"
}

%% ========================================
%% 좋아요
%% ========================================
LIKES {
bigint id PK "자동증가"
bigint user_id FK "사용자 ID"
bigint product_id FK "상품 ID"
datetime created_at "좋아요 시각"
%% UNIQUE(user_id, product_id)
}

%% ========================================
%% 장바구니
%% ========================================
CART {
bigint id PK "자동증가"
bigint user_id FK "사용자 ID"
bigint product_id FK "상품 ID"
int quantity "수량"
datetime created_at "담은 일시"
%% UNIQUE(user_id, product_id)
}

%% ========================================
%% 주문
%% ========================================
ORDERS {
bigint id PK "자동증가"
varchar order_number UK "주문번호"
bigint user_id FK "주문자 ID"
int total_amount "총 금액"
varchar order_status "PENDING/CONFIRMED/CANCELLED"
datetime ordered_at "주문일시"
datetime confirmed_at "주문확인일시"
datetime cancelled_at "취소일시"
}

%% ========================================
%% 주문 항목 (스냅샷)
%% ========================================
ORDER_ITEMS {
bigint id PK "자동증가"
bigint order_id FK "주문 ID"
bigint product_id FK "상품 ID"
int quantity "수량"
varchar product_name "상품명 (스냅샷)"
int product_price "단가 (스냅샷)"
varchar brand_name "브랜드명 (스냅샷)"
varchar image_url "이미지 (스냅샷)"
}

%% ========================================
%% 관계
%% ========================================
%% 사용자 중심 관계
USERS ||--o{ LIKES : "관심등록"
USERS ||--o{ CART : "장바구니담기"
USERS ||--o{ ORDERS : "주문수행"

%% 상품/브랜드 관계
BRANDS ||--o{ PRODUCTS : "상품입점"
PRODUCTS ||--o{ LIKES : "좋아요받음"
PRODUCTS ||--o{ CART : "장바구니에포함"
PRODUCTS ||--o{ ORDER_ITEMS : "주문상품내역"

%% 주문 상세
ORDERS ||--|{ ORDER_ITEMS : "포함내용"