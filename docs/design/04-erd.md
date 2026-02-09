# ERD

erDiagram
%% ========================================
%% 사용자
%% ========================================
USERS {
bigint id PK "자동증가"
varchar login_id UK "로그인 ID (unique)"
varchar password "BCrypt 해시"
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
        datetime deleted_at "삭제일시 (Soft Delete)"
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
        varchar image_url "상품 이미지 URL"
        int version "낙관적 락 버전"
        int likes_count "좋아요 수 (비정규화)"
        datetime created_at "등록일시"
        datetime updated_at "수정일시"
        datetime deleted_at "삭제일시 (Soft Delete)"
    }
    
    %% ========================================
    %% 좋아요
    %% ========================================
    LIKES {
        bigint id PK "자동증가"
        bigint user_id FK "사용자 ID"
        bigint product_id FK "상품 ID"
        datetime created_at "좋아요 누른 시각"
    }
    
    %% ========================================
    %% 주문
    %% ========================================
    ORDERS {
        bigint id PK "자동증가"
        bigint user_id FK "주문자 ID"
        int total_amount "총 주문 금액"
        varchar status "주문 상태"
        datetime ordered_at "주문일시"
    }
    
    %% ========================================
    %% 주문 항목 (스냅샷)
    %% ========================================
    ORDER_ITEMS {
        bigint id PK "자동증가"
        bigint order_id FK "주문 ID"
        bigint product_id FK "상품 ID (참조용)"
        int quantity "주문 수량"
        varchar product_name_snapshot "주문 당시 상품명"
        int product_price_snapshot "주문 당시 가격"
        varchar brand_name_snapshot "주문 당시 브랜드명"
        varchar image_url_snapshot "주문 당시 이미지 URL"
    }
    
    %% ========================================
    %% 관계
    %% ========================================
    USERS ||--o{ LIKES : "좋아요 누름"
    USERS ||--o{ ORDERS : "주문함"
    
    BRANDS ||--o{ PRODUCTS : "보유"
    
    PRODUCTS ||--o{ LIKES : "좋아요 받음"
    PRODUCTS ||--o{ ORDER_ITEMS : "주문에 포함됨"
    
    ORDERS ||--|{ ORDER_ITEMS : "주문 항목 포함"

