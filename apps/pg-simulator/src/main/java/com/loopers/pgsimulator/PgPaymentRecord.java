package com.loopers.pgsimulator;

public class PgPaymentRecord {

    public enum PgPaymentStatus {
        PROCESSING, SUCCESS, LIMIT_EXCEEDED, INVALID_CARD
    }

    private final String transactionId;
    private final String orderId;
    private final String userId;
    private final String cardType;
    private final String cardNo;
    private final Long amount;
    private final String callbackUrl;
    private PgPaymentStatus status;
    private String failureReason;

    public PgPaymentRecord(String transactionId, String orderId, String userId,
                           String cardType, String cardNo, Long amount, String callbackUrl) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.userId = userId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.callbackUrl = callbackUrl;
        this.status = PgPaymentStatus.PROCESSING;
    }

    public String getTransactionId() { return transactionId; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getCardType() { return cardType; }
    public String getCardNo() { return cardNo; }
    public Long getAmount() { return amount; }
    public String getCallbackUrl() { return callbackUrl; }
    public PgPaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }

    public void setStatus(PgPaymentStatus status) { this.status = status; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
