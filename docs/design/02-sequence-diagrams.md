# Sequence Diagrams


# 좋아요 등록/취소
sequenceDiagram
title 좋아요 등록/취소
actor User as 사용자
participant API as Like API
participant LikeService as Like Service
participant LikeRepository as Like Repository
participant ProductRepository as Product Repository
participant DB as Database

    %% ========================================
    %% 좋아요 등록
    %% ========================================
    User->>API: 좋아요 등록
    activate API
    
    API->>LikeService: 좋아요 등록 처리
    activate LikeService
    
    LikeService->>LikeRepository: 중복 확인 (조회)
    activate LikeRepository
    LikeRepository->>DB: SELECT 
    DB-->>LikeRepository: 결과 반환
    
    alt 이미 좋아요함
        LikeRepository-->>LikeService: 중복 예외 던짐
        deactivate LikeRepository
        LikeService-->>API: 에러 응답
        API-->>User: 400 Bad Request (실패)
    else 좋아요 안함
        LikeService->>LikeRepository: 좋아요 저장
        activate LikeRepository
        LikeRepository->>DB: INSERT
        deactivate LikeRepository
        
        LikeService->>ProductRepository: 좋아요 수 증가
        activate ProductRepository
        ProductRepository->>DB: UPDATE (likes_count+1)
        deactivate ProductRepository
        
        LikeService-->>API: 처리 완료
        deactivate LikeService
        API-->>User: 200 OK (성공)
    end
    deactivate API

    %% ========================================
    %% 좋아요 취소
    %% ========================================
    User->>API: 좋아요 취소
    activate API
    
    API->>LikeService: 좋아요 취소 처리
    activate LikeService
    
    LikeService->>LikeRepository: 좋아요 삭제
    activate LikeRepository
    LikeRepository->>DB: DELETE
    deactivate LikeRepository
    
    LikeService->>ProductRepository: 좋아요 수 감소
    activate ProductRepository
    ProductRepository->>DB: UPDATE (likes_count-1)
    deactivate ProductRepository
    
    LikeService-->>API: 처리 완료
    deactivate LikeService
    API-->>User: 200 OK (성공)
    deactivate API
# 주문 생성(재고확인 및 차감)
sequenceDiagram
title 주문 생성 (재고 확인 및 차감)

    actor User as 사용자
    participant API as Order API
    participant OrderService as Order Service
    participant ProductRepository as Product Repository
    participant OrderRepository as Order Repository
    participant CartRepository as Cart Repository
    participant DB as Database
    
    User->>API: 주문 생성 요청
    
    API->>OrderService: 주문 처리
    activate OrderService
    
    OrderService->>ProductRepository: 재고 확인 및 차감
    activate ProductRepository
    ProductRepository->>DB: 재고 조회 (FOR UPDATE)
    
    alt 재고 부족
        DB-->>ProductRepository: 부족
        ProductRepository-->>OrderService: 재고 부족 예외
        OrderService-->>API: 실패
        API-->>User: 실패
    else 재고 충분
        ProductRepository->>DB: 재고 차감 (version++)
        DB-->>ProductRepository: 성공
        deactivate ProductRepository
        
        OrderService->>OrderRepository: 주문 저장
        activate OrderRepository
        OrderRepository->>DB: 주문 생성 (ORDERS)
        OrderRepository->>DB: 주문 항목 저장 (ORDER_ITEMS, 스냅샷)
        DB-->>OrderRepository: 성공
        deactivate OrderRepository
        
        OrderService->>CartRepository: 장바구니 삭제
        activate CartRepository
        CartRepository->>DB: 장바구니 삭제
        deactivate CartRepository
        
        deactivate OrderService
        API-->>User: 성공
    end
# 주문상태 변경
sequenceDiagram
title 주문 상태 변경 (PENDING → CONFIRMED → CANCELLED)

    actor Admin as 관리자
    actor User as 사용자
    participant API as Order API
    participant OrderService as Order Service
    participant OrderRepository as Order Repository
    participant ProductRepository as Product Repository
    participant DB as Database
    
    Note over API,DB: 관리자: 주문 확인
    Admin->>API: 주문 확인 요청
    
    API->>OrderService: 주문 확인 처리
    activate OrderService
    
    OrderService->>OrderRepository: 주문 상태 조회
    activate OrderRepository
    OrderRepository->>DB: 주문 조회
    
    alt 이미 CONFIRMED
        DB-->>OrderRepository: CONFIRMED
        OrderRepository-->>OrderService: 이미 확인됨
        deactivate OrderRepository
        OrderService-->>API: 실패
        API-->>Admin: 실패
    else PENDING
        DB-->>OrderRepository: PENDING
        
        OrderService->>OrderRepository: 상태 변경
        activate OrderRepository
        OrderRepository->>DB: CONFIRMED로 변경
        deactivate OrderRepository
        
        deactivate OrderService
        API-->>Admin: 성공
    end
    
    Note over API,DB: 사용자: 주문 취소
    User->>API: 주문 취소 요청
    
    API->>OrderService: 주문 취소 처리
    activate OrderService
    
    OrderService->>OrderRepository: 주문 상태 조회
    activate OrderRepository
    OrderRepository->>DB: 주문 조회
    
    alt CONFIRMED 상태
        DB-->>OrderRepository: CONFIRMED
        OrderRepository-->>OrderService: 취소 불가
        deactivate OrderRepository
        OrderService-->>API: 실패
        API-->>User: 실패
    else PENDING
        DB-->>OrderRepository: PENDING
        
        OrderService->>ProductRepository: 재고 복구
        activate ProductRepository
        ProductRepository->>DB: 재고 복구
        deactivate ProductRepository
        
        OrderService->>OrderRepository: 상태 변경
        activate OrderRepository
        OrderRepository->>DB: CANCELLED로 변경
        deactivate OrderRepository
        
        deactivate OrderService
        API-->>User: 성공
    end