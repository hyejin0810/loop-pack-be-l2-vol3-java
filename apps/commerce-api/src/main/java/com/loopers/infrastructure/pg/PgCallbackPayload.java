package com.loopers.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PgCallbackPayload(
    String transactionId,
    String orderId,
    String status,
    String failureReason
) {}
