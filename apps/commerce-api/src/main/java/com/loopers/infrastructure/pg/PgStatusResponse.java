package com.loopers.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PgStatusResponse(
    String transactionId,
    String orderId,
    String status,
    String failureReason
) {}
