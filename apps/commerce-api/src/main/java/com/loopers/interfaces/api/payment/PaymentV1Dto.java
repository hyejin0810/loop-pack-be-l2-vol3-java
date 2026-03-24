package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentStatus;

public class PaymentV1Dto {

    public record PaymentRequest(
        String preOrderId,
        String cardType,
        String cardNo
    ) {}

    public record PaymentResponse(
        Long paymentId,
        String preOrderId,
        Long orderId,
        String transactionId,
        PaymentStatus status,
        String cardType,
        Long amount,
        String failureReason
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                info.paymentId(),
                info.preOrderId(),
                info.orderId(),
                info.transactionId(),
                info.status(),
                info.cardType(),
                info.amount(),
                info.failureReason()
            );
        }
    }
}
