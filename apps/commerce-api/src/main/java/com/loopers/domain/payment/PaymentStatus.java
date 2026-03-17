package com.loopers.domain.payment;

public enum PaymentStatus {
    PENDING,     // 결제 요청 대기 중 (PG 콜백 미수신)
    COMPLETED,   // 결제 완료
    FAILED,      // 결제 실패 (한도 초과 / 잘못된 카드)
    TIMEOUT      // PG 요청 타임아웃
}
