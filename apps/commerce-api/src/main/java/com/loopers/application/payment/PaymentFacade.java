package com.loopers.application.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.pg.PgCallbackPayload;
import com.loopers.infrastructure.pg.PgPaymentCaller;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgStatusResponse;
import com.loopers.infrastructure.pg.PgUnavailableException;
import com.loopers.infrastructure.preorder.PreOrder;
import com.loopers.infrastructure.preorder.PreOrderCacheService;
import com.loopers.infrastructure.preorder.PreOrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentService paymentService;
    private final UserService userService;
    private final ProductService productService;
    private final IssuedCouponRepository issuedCouponRepository;
    private final PreOrderCacheService preOrderCacheService;
    private final PgPaymentCaller pgPaymentCaller;
    private final ObjectMapper objectMapper;
    private final PaymentSagaService paymentSagaService;

    @Value("${pg.simulator.callback-url:http://localhost:8080/api/v1/payments/callback}")
    private String callbackUrl;

    @Transactional
    public PaymentInfo requestPayment(String loginId, String rawPassword,
                                      String preOrderId, String cardType, String cardNo) {
        User user = userService.authenticate(loginId, rawPassword);

        PreOrder preOrder = preOrderCacheService.get(preOrderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "가주문을 찾을 수 없거나 만료되었습니다."));
        if (!preOrder.getUserId().equals(user.getId())) {
            throw new CoreException(ErrorType.NOT_FOUND, "가주문을 찾을 수 없습니다.");
        }
        if (preOrder.getStatus().name().equals("PAYMENT_REQUESTED")) {
            throw new CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중입니다.");
        }

        // 재고 먼저 차감 — 결제 승인 후 품절 방지
        // 데드락 방지: 상품 ID 오름차순
        List<PreOrderItem> sortedItems = preOrder.getItems().stream()
            .sorted(Comparator.comparingLong(PreOrderItem::productId))
            .toList();
        for (PreOrderItem item : sortedItems) {
            productService.decrementStock(item.productId(), item.quantity());
        }

        // 쿠폰 선차감 — 동일 쿠폰 중복 사용 방지
        // 실패 시 @Transactional 롤백으로 재고 차감도 함께 되돌려짐
        if (preOrder.getIssuedCouponId() != null) {
            IssuedCoupon coupon = issuedCouponRepository
                .findByIdForUpdate(preOrder.getIssuedCouponId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
            coupon.use();
        }

        // pre-order 스냅샷을 Payment에 저장 (Redis 만료 시 SAGA 복구용 — Write-through 보완)
        String snapshot = serializeSnapshot(preOrder);
        Payment payment = paymentService.createPayment(
            preOrderId, user.getId(), cardType, cardNo, preOrder.getTotalAmount(), snapshot
        );

        // 결제 시작 → 수정 불가 상태로 변경
        preOrder.markPaymentRequested();
        preOrderCacheService.save(preOrder);

        PgPaymentRequest pgRequest = new PgPaymentRequest(
            preOrderId, cardType, cardNo, preOrder.getTotalAmount(), callbackUrl
        );

        try {
            PgPaymentResponse pgResponse = pgPaymentCaller.requestPayment(
                String.valueOf(user.getId()), pgRequest
            );
            payment.markProcessing(pgResponse.transactionId());
            log.info("[Payment] PG 결제 요청 성공: paymentId={}, transactionId={}",
                payment.getId(), pgResponse.transactionId());
        } catch (PgUnavailableException e) {
            // TIMEOUT: 재고는 차감된 상태 유지 — 스케줄러가 PG 상태 확인 후 복구 처리
            payment.timeout();
            log.warn("[Payment] PG 결제 요청 실패 (timeout/circuit open): paymentId={}", payment.getId());
        }

        return PaymentInfo.from(paymentService.save(payment));
    }

    /**
     * PG 콜백 수신 — TX1 (빠른 응답)
     *
     * 트랜잭션 없이 콜백 수신 즉시 PG에 응답을 반환한다.
     * SAGA는 PaymentSagaService에서 별도 트랜잭션(TX2)으로 실행된다.
     *
     * 멱등성: isFinalized() 체크로 중복 콜백 무시.
     */
    public void handleCallback(PgCallbackPayload payload) {
        Payment payment = paymentService.getPaymentByTransactionId(payload.transactionId());

        if (payment.isFinalized()) {
            log.warn("[Payment] 중복 콜백 무시 (이미 처리 완료): transactionId={}", payload.transactionId());
            return;
        }

        switch (payload.status()) {
            case "SUCCESS" -> paymentSagaService.executeOnSuccess(payment.getId());
            case "LIMIT_EXCEEDED", "INVALID_CARD" ->
                paymentSagaService.executeOnFailure(payment.getId(), payload.failureReason());
            default -> log.warn("[Payment] 콜백 수신 - 알 수 없는 상태: status={}", payload.status());
        }
    }

    /**
     * 콜백 미수신 시 수동 동기화.
     * PaymentSyncScheduler에서도 호출된다.
     */
    @Transactional
    public PaymentInfo syncPaymentStatus(String loginId, String rawPassword, Long paymentId) {
        User user = userService.authenticate(loginId, rawPassword);
        Payment payment = paymentService.getPayment(paymentId);

        if (!payment.getUserId().equals(user.getId())) {
            throw new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        }

        syncPayment(payment, String.valueOf(user.getId()));
        return PaymentInfo.from(paymentService.getPayment(paymentId));
    }

    /** 스케줄러에서 사용: 인증 없이 paymentId로 동기화 */
    @Transactional
    public void syncPaymentById(Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        syncPayment(payment, String.valueOf(payment.getUserId()));
    }

    private void syncPayment(Payment payment, String userId) {
        String statusName = payment.getStatus().name();
        if (!statusName.equals("PENDING") && !statusName.equals("TIMEOUT")) {
            return;
        }

        List<PgStatusResponse> pgResponses = pgPaymentCaller.getPaymentsByOrderId(
            userId, payment.getPreOrderId()
        );
        if (pgResponses.isEmpty()) {
            log.warn("[Payment] PG 결제 정보 없음: preOrderId={}", payment.getPreOrderId());
            return;
        }

        PgStatusResponse pgStatus = pgResponses.get(0);
        switch (pgStatus.status()) {
            case "SUCCESS" -> {
                if (payment.getTransactionId() == null) {
                    payment.markProcessing(pgStatus.transactionId());
                    paymentService.save(payment);
                }
                paymentSagaService.executeOnSuccess(payment.getId());
            }
            case "LIMIT_EXCEEDED", "INVALID_CARD" ->
                paymentSagaService.executeOnFailure(payment.getId(), pgStatus.failureReason());
            default -> log.info("[Payment] 아직 처리 중: preOrderId={}", payment.getPreOrderId());
        }
    }

    private String serializeSnapshot(PreOrder preOrder) {
        try {
            return objectMapper.writeValueAsString(preOrder);
        } catch (JsonProcessingException e) {
            log.warn("[Payment] pre-order 스냅샷 직렬화 실패: preOrderId={}", preOrder.getPreOrderId(), e);
            return null;
        }
    }
}
