# Sequence Diagrams


# 좋아요 등록/취소


```mermaid
sequenceDiagram
title 좋아요 처리 (회원 전용)
actor User as 사용자
participant Auth as Auth Filter
participant API as Like API
participant Service as Like Service
participant Repo as Like Repository
participant ProdRepo as Product Repository

    User->>Auth: 좋아요/취소 요청 (X-Loopers-LoginId, LoginPw)
    Auth->>Auth: 회원 인증 및 UserId 추출
    Auth->>API: 요청 전달 (UserId, ProductId)
    
    activate API
    API->>Service: 좋아요/취소 로직 실행
    activate Service
    
    alt 등록 요청
        Service->>Repo: 기존 존재 여부 확인
        Repo-->>Service: 결과 반환
        alt 미존재
            Service->>Repo: INSERT (Like)
            Service->>ProdRepo: UPDATE (likes_count + 1)
            Service-->>API: 성공
        else 이미 존재
            Service-->>API: 400 Bad Request
        end
    else 취소 요청
        Service->>Repo: DELETE (Like)
        Note right of Service: 삭제 성공 시에만 count 감소
        Service->>ProdRepo: UPDATE (likes_count - 1)
        Service-->>API: 성공
    end
    
    deactivate Service
    API-->>User: 200 OK
    deactivate API
    
 ```




# 주문 생성(재고확인 및 차감)

```mermaid

sequenceDiagram
title 주문 생성 (재고 차감 및 장바구니 삭제)

    actor User as 사용자
    participant API as Order API
    participant Service as Order Service
    participant ProdRepo as Product Repository
    participant OrderRepo as Order Repository
    participant CartRepo as Cart Repository

    User->>API: 주문 생성 요청
    activate API

    API->>Service: 주문 생성 트랜잭션 시작
    activate Service

    Service->>ProdRepo: 재고 차감 요청

    ProdRepo-->>Service: 성공 여부 반환

    alt 재고 부족
        Service-->>API: 재고 부족 예외 던짐
        API-->>User: 400 Bad Request (품절)
    else 재고 충분 및 차감 완료
        Service->>OrderRepo: 주문(Order) & 상세(OrderItems) 저장
        OrderRepo-->>Service: 저장 완료

        Service->>CartRepo: 장바구니 데이터 삭제
        CartRepo-->>Service: 삭제 완료

        Service-->>API: 주문 성공 응답
        deactivate Service
        API-->>User: 201 Created (주문 완료)
    end
    deactivate API
```



# 주문상태 변경


```mermaid
sequenceDiagram
title 주문 상태 변경 (관리자 승인 / 사용자 취소)

    actor Actor as 관리자/사용자
    participant API as Order API
    participant Service as Order Service
    participant OrderRepo as Order Repository
    participant ProdRepo as Product Repository

    Actor->>API: 상태 변경 요청 (Approve / Cancel)
    activate API
    
    API->>Service: 주문 상태 변경 처리
    activate Service
    
    Service->>OrderRepo: 현재 주문 정보 조회
    OrderRepo-->>Service: Order Entity 반환
    
    alt 상태가 PENDING이 아님
        Service-->>API: 변경 불가 예외 (400)
        API-->>Actor: 실패 (이미 처리된 주문입니다)
    else 상태가 PENDING임
        alt 관리자 승인 (Approve)
            Service->>OrderRepo: 상태를 'CONFIRMED'로 업데이트
        else 사용자 취소 (Cancel)
            Service->>ProdRepo: 재고 복구 (stock + n)
            Service->>OrderRepo: 상태를 'CANCELLED'로 업데이트
        end
        Service-->>API: 성공 응답
        deactivate Service
        API-->>Actor: 200 OK (처리 완료)
    end
    deactivate API
```