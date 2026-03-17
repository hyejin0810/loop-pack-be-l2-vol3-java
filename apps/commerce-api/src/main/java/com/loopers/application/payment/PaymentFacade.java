package com.loopers.application.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final IssuedCouponRepository issuedCouponRepository;
    private final PreOrderCacheService preOrderCacheService;
    private final PgPaymentCaller pgPaymentCaller;
    private final ObjectMapper objectMapper;

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
            payment.timeout();
            log.warn("[Payment] PG 결제 요청 실패 (timeout/circuit open): paymentId={}", payment.getId());
        }

        return PaymentInfo.from(paymentService.save(payment));
    }

    /**
     * PG 콜백 수신 — SAGA (Choreography) 패턴으로 분산 트랜잭션 처리
     *
     * SUCCESS 시 단계:
     *   Step 1: 재고 차감    (실패 시 → Payment FAILED)
     *   Step 2: 잔액 차감    (실패 시 → 재고 복구 + Payment FAILED)
     *   Step 3: 쿠폰 사용    (실패 시 → 재고/잔액 복구 + Payment FAILED)
     *   Step 4: Order 생성   (실패 시 → 재고/잔액/쿠폰 복구 + Payment FAILED)
     *   Step 5: Payment 완료
     *   Step 6: Redis pre-order 삭제
     */
    @Transactional
    public void handleCallback(PgCallbackPayload payload) {
        Payment payment = paymentService.getPaymentByTransactionId(payload.transactionId());

        switch (payload.status()) {
            case "SUCCESS" -> executeSaga(payment);
            case "LIMIT_EXCEEDED", "INVALID_CARD" -> {
                payment.fail(payload.failureReason());
                paymentService.save(payment);
                log.info("[Payment] 콜백 수신 - 결제 실패: transactionId={}, reason={}",
                    payload.transactionId(), payload.failureReason());
            }
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
                executeSaga(payment);
            }
            case "LIMIT_EXCEEDED", "INVALID_CARD" -> {
                payment.fail(pgStatus.failureReason());
                paymentService.save(payment);
            }
            default -> log.info("[Payment] 아직 처리 중: preOrderId={}", payment.getPreOrderId());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SAGA 실행 (보상 트랜잭션 포함)
    // ─────────────────────────────────────────────────────────────

    private void executeSaga(Payment payment) {
        PreOrder preOrder = resolvePreOrder(payment);
        if (preOrder == null) {
            payment.fail("가주문 정보를 복구할 수 없습니다.");
            paymentService.save(payment);
            log.error("[SAGA] pre-order 복구 실패: preOrderId={}", payment.getPreOrderId());
            return;
        }

        // 데드락 방지: 상품 ID 오름차순
        List<PreOrderItem> sortedItems = preOrder.getItems().stream()
            .sorted(Comparator.comparingLong(PreOrderItem::productId))
            .toList();

        // Step 1: 재고 차감
        List<PreOrderItem> deductedStock = new ArrayList<>();
        try {
            for (PreOrderItem item : sortedItems) {
                productService.decrementStock(item.productId(), item.quantity());
                deductedStock.add(item);
            }
        } catch (Exception e) {
            log.error("[SAGA] Step1 재고 차감 실패: preOrderId={}", payment.getPreOrderId(), e);
            deductedStock.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            payment.fail("재고 차감 실패: " + e.getMessage());
            paymentService.save(payment);
            return;
        }

        // Step 2: 잔액 차감
        User user;
        try {
            user = userService.getUser(payment.getUserId());
            user.deductBalance(preOrder.getTotalAmount());
        } catch (Exception e) {
            log.error("[SAGA] Step2 잔액 차감 실패: preOrderId={}", payment.getPreOrderId(), e);
            deductedStock.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            payment.fail("잔액 차감 실패: " + e.getMessage());
            paymentService.save(payment);
            return;
        }

        // Step 3: 쿠폰 사용
        Long confirmedIssuedCouponId = null;
        if (preOrder.getIssuedCouponId() != null) {
            try {
                IssuedCoupon issuedCoupon = issuedCouponRepository
                    .findByIdForUpdate(preOrder.getIssuedCouponId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
                issuedCoupon.use();
                confirmedIssuedCouponId = issuedCoupon.getId();
            } catch (ObjectOptimisticLockingFailureException | CoreException e) {
                log.error("[SAGA] Step3 쿠폰 사용 실패: preOrderId={}", payment.getPreOrderId());
                deductedStock.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
                user.restoreBalance(preOrder.getTotalAmount());
                payment.fail("쿠폰 처리 실패");
                paymentService.save(payment);
                return;
            }
        }

        // Step 4: Order DB 생성 (CONFIRMED)
        Order order;
        try {
            order = confirmedIssuedCouponId != null
                ? orderService.createOrder(payment.getUserId(), preOrder.getPreOrderId(),
                    preOrder.getOriginalAmount(), preOrder.getDiscountAmount(), confirmedIssuedCouponId)
                : orderService.createOrder(payment.getUserId(), preOrder.getPreOrderId(),
                    preOrder.getOriginalAmount());

            orderService.approveOrder(order.getId());

            for (PreOrderItem item : preOrder.getItems()) {
                orderService.createOrderItem(
                    order.getId(), item.productId(), item.productName(),
                    item.brandName(), item.imageUrl(), item.price(), item.quantity()
                );
            }
        } catch (Exception e) {
            log.error("[SAGA] Step4 Order 생성 실패: preOrderId={}", payment.getPreOrderId(), e);
            deductedStock.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            user.restoreBalance(preOrder.getTotalAmount());
            final Long finalCouponId = confirmedIssuedCouponId;
            if (finalCouponId != null) {
                issuedCouponRepository.findById(finalCouponId).ifPresent(IssuedCoupon::restore);
            }
            payment.fail("주문 생성 실패: " + e.getMessage());
            paymentService.save(payment);
            return;
        }

        // Step 5: Payment 완료
        payment.linkOrder(order.getId());
        payment.complete();
        paymentService.save(payment);

        // Step 6: Redis pre-order 삭제
        preOrderCacheService.delete(preOrder.getPreOrderId());

        log.info("[SAGA] 완료: preOrderId={}, orderId={}, paymentId={}",
            preOrder.getPreOrderId(), order.getId(), payment.getId());
    }

    /**
     * pre-order 복구 전략:
     * 1순위: Redis (정상 경로)
     * 2순위: Payment.preOrderSnapshot (DB — Redis 만료/장애 시 복구)
     */
    private PreOrder resolvePreOrder(Payment payment) {
        Optional<PreOrder> fromRedis = preOrderCacheService.get(payment.getPreOrderId());
        if (fromRedis.isPresent()) {
            return fromRedis.get();
        }
        log.warn("[SAGA] Redis pre-order 만료, DB 스냅샷으로 복구: preOrderId={}", payment.getPreOrderId());
        if (payment.getPreOrderSnapshot() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payment.getPreOrderSnapshot(), PreOrder.class);
        } catch (Exception e) {
            log.error("[SAGA] 스냅샷 역직렬화 실패: preOrderId={}", payment.getPreOrderId(), e);
            return null;
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
