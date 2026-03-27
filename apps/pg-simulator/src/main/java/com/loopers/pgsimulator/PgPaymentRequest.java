package com.loopers.pgsimulator;

public record PgPaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String callbackUrl
) {}
