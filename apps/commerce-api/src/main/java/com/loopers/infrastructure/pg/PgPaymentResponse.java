package com.loopers.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PgPaymentResponse(
    String transactionId,
    String status
) {}
