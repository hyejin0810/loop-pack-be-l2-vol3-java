package com.loopers.infrastructure.preorder;

public enum PreOrderStatus {
    DRAFT,              // 수정 가능
    PAYMENT_REQUESTED   // PG 호출 중, 수정 불가
}
