# Sequence Diagrams

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







------------------------
--정규한 data
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
    }
    
    %% ========================================
    %% 쿠폰 마스터
    %% ========================================
    COUPONS {
        bigint id PK "자동증가"
        varchar code UK "쿠폰 코드"
        varchar name "쿠폰명"
        varchar discount_type "FIXED/PERCENT"
        int discount_value "할인 값"
        int max_discount_amount "최대 할인 금액"
        int min_order_amount "최소 주문 금액"
        int issue_limit "발급 제한"
        int issued_count "발급 수"
        datetime start_at "시작일시"
        datetime end_at "종료일시"
        datetime created_at "생성일시"
    }
    
    %% ========================================
    %% 사용자 쿠폰
    %% ========================================
    USER_COUPONS {
        bigint id PK "자동증가"
        bigint user_id FK "사용자 ID"
        bigint coupon_id FK "쿠폰 ID"
        varchar status "AVAILABLE/USED/EXPIRED"
        datetime issued_at "발급일시"
        datetime used_at "사용일시"
        datetime expired_at "만료일시"
    }
    
    %% ========================================
    %% 주문 (핵심 - 최소 스냅샷)
    %% ========================================
    ORDERS {
        bigint id PK "자동증가"
        varchar order_number UK "주문번호"
        bigint user_id FK "주문자 ID"
        int total_amount "상품 총액"
        int discount_amount "할인 금액"
        int final_amount "최종 결제 금액"
        bigint user_coupon_id FK "사용된 쿠폰 ID"
        varchar order_status "주문 상태"
        datetime ordered_at "주문일시"
        datetime paid_at "결제일시"
        datetime cancelled_at "취소일시"
    }
    
    %% ========================================
    %% 주문 항목 (필수 스냅샷만)
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
    %% 결제
    %% ========================================
    PAYMENTS {
        bigint id PK "자동증가"
        bigint order_id FK "주문 ID"
        int amount "결제 금액"
        varchar method "결제 수단"
        varchar pg_provider "PG사"
        varchar pg_token "PG 토큰"
        varchar status "결제 상태"
        datetime paid_at "결제일시"
        datetime created_at "생성일시"
    }
    
    %% ========================================
    %% 관계
    %% ========================================
    USERS ||--o{ LIKES : ""
    USERS ||--o{ ORDERS : ""
    USERS ||--o{ USER_COUPONS : ""
    
    BRANDS ||--o{ PRODUCTS : ""
    
    PRODUCTS ||--o{ LIKES : ""
    PRODUCTS ||--o{ ORDER_ITEMS : ""
    
    COUPONS ||--o{ USER_COUPONS : ""
    
    USER_COUPONS ||--o{ ORDERS : ""
    
    ORDERS ||--|{ ORDER_ITEMS : ""
    ORDERS ||--o| PAYMENTS : ""