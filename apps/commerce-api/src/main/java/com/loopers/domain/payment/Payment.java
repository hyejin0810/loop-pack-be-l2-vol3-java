package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "payments")
@Getter
public class Payment extends BaseEntity {

    /** 가주문 ID (UUID) — PreOrder 식별자, PG 요청 전부터 사용 */
    @Column(name = "pre_order_id", nullable = false, length = 50, unique = true)
    private String preOrderId;

    /** 확정 주문 ID — SAGA 완료 후 Order가 생성되면 채워짐 */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "card_type", nullable = false, length = 30)
    private String cardType;

    @Column(name = "card_no", nullable = false, length = 30)
    private String cardNo;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    /**
     * 가주문 스냅샷 (JSON) — Redis pre-order 만료/장애 시 SAGA 복구에 사용
     *
     * Write-through 전략의 일부:
     * Redis-only 저장의 내구성 한계를 보완하기 위해
     * 결제 요청 시 pre-order 데이터를 DB에도 함께 보존한다.
     */
    @Column(name = "pre_order_snapshot", columnDefinition = "TEXT")
    private String preOrderSnapshot;

    protected Payment() {}

    public Payment(String preOrderId, Long userId, String cardType, String cardNo,
                   Long amount, String preOrderSnapshot) {
        this.preOrderId = preOrderId;
        this.userId = userId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.preOrderSnapshot = preOrderSnapshot;
        this.status = PaymentStatus.PENDING;
    }

    public void markProcessing(String transactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "대기 중인 결제만 처리 시작할 수 있습니다.");
        }
        this.transactionId = transactionId;
    }

    /** SAGA Step 3 완료 후 확정 주문 ID를 연결한다. */
    public void linkOrder(Long orderId) {
        this.orderId = orderId;
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void timeout() {
        this.status = PaymentStatus.TIMEOUT;
        this.failureReason = "PG 요청 타임아웃";
    }

    /** 이미 최종 처리된 결제인지 확인 — 중복 콜백 멱등성 보장에 사용 */
    public boolean isFinalized() {
        return this.status == PaymentStatus.COMPLETED || this.status == PaymentStatus.FAILED;
    }
}
