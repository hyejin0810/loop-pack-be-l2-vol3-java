package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

public record PaymentInfo(
    Long paymentId,
    String preOrderId,
    Long orderId,
    String transactionId,
    PaymentStatus status,
    String cardType,
    Long amount,
    String failureReason
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
            payment.getId(),
            payment.getPreOrderId(),
            payment.getOrderId(),
            payment.getTransactionId(),
            payment.getStatus(),
            payment.getCardType(),
            payment.getAmount(),
            payment.getFailureReason()
        );
    }
}
