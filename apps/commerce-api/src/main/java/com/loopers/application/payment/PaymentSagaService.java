package com.loopers.application.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.preorder.PreOrder;
import com.loopers.infrastructure.preorder.PreOrderCacheService;
import com.loopers.infrastructure.preorder.PreOrderItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SAGA 실행을 전담하는 컴포넌트.
 *
 * handleCallback()과 트랜잭션을 분리하기 위해 별도 Bean으로 분리.
 * - TX1: handleCallback() — PG에 즉시 응답 (트랜잭션 없음)
 * - TX2: PaymentSagaService — SAGA 로직을 별도 트랜잭션으로 실행
 *
 * 멱등성: executeOnSuccess/executeOnFailure 진입 시 isFinalized() 체크로 중복 실행 방지.
 */
@RequiredArgsConstructor
@Component
public class PaymentSagaService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaService.class);

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final IssuedCouponRepository issuedCouponRepository;
    private final PreOrderCacheService preOrderCacheService;
    private final ObjectMapper objectMapper;

    /**
     * PG 결제 성공 시 SAGA 실행.
     * 재고는 requestPayment() 시점에 이미 차감되어 있음.
     *
     * Step 1: 잔액 차감    (실패 시 → 재고 + 쿠폰 복구 + Payment FAILED)
     * Step 2: Order 생성   (실패 시 → 재고 + 쿠폰 + 잔액 복구 + Payment FAILED)
     * Step 3: Payment 완료
     * Step 4: Redis pre-order 삭제
     */
    @Transactional
    public void executeOnSuccess(Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);

        if (payment.isFinalized()) {
            log.warn("[SAGA] 이미 완료된 결제 - 중복 실행 무시: paymentId={}", paymentId);
            return;
        }

        PreOrder preOrder = resolvePreOrder(payment);
        if (preOrder == null) {
            payment.fail("가주문 정보를 복구할 수 없습니다.");
            paymentService.save(payment);
            log.error("[SAGA] pre-order 복구 실패: preOrderId={}", payment.getPreOrderId());
            return;
        }

        // 데드락 방지: 상품 ID 오름차순 (보상 트랜잭션용)
        List<PreOrderItem> sortedItems = preOrder.getItems().stream()
            .sorted(Comparator.comparingLong(PreOrderItem::productId))
            .toList();

        // Step 1: 잔액 차감 (실패 시 → 재고 + 쿠폰 복구 + Payment FAILED)
        // 쿠폰은 requestPayment() 시점에 이미 선차감되어 있음
        User user;
        try {
            user = userService.getUser(payment.getUserId());
            user.deductBalance(preOrder.getTotalAmount());
        } catch (Exception e) {
            log.error("[SAGA] Step1 잔액 차감 실패: preOrderId={}", payment.getPreOrderId(), e);
            sortedItems.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            restoreCoupon(preOrder);
            payment.fail("잔액 차감 실패: " + e.getMessage());
            paymentService.save(payment);
            return;
        }

        // Step 2: Order 생성 (실패 시 → 재고 + 쿠폰 + 잔액 복구 + Payment FAILED)
        Order order;
        try {
            order = preOrder.getIssuedCouponId() != null
                ? orderService.createOrder(payment.getUserId(), preOrder.getPreOrderId(),
                    preOrder.getOriginalAmount(), preOrder.getDiscountAmount(), preOrder.getIssuedCouponId())
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
            log.error("[SAGA] Step2 Order 생성 실패: preOrderId={}", payment.getPreOrderId(), e);
            sortedItems.forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            restoreCoupon(preOrder);
            user.restoreBalance(preOrder.getTotalAmount());
            payment.fail("주문 생성 실패: " + e.getMessage());
            paymentService.save(payment);
            return;
        }

        // Step 4: Payment 완료
        payment.linkOrder(order.getId());
        payment.complete();
        paymentService.save(payment);

        // Step 5: Redis pre-order 삭제
        preOrderCacheService.delete(preOrder.getPreOrderId());

        log.info("[SAGA] 완료: preOrderId={}, orderId={}, paymentId={}",
            preOrder.getPreOrderId(), order.getId(), payment.getId());
    }

    /** PG 결제 실패 시 차감된 재고를 복구하고 Payment를 FAILED로 처리. */
    @Transactional
    public void executeOnFailure(Long paymentId, String reason) {
        Payment payment = paymentService.getPayment(paymentId);

        if (payment.isFinalized()) {
            log.warn("[Payment] 이미 처리된 결제 - 중복 실행 무시: paymentId={}", paymentId);
            return;
        }

        PreOrder preOrder = resolvePreOrder(payment);
        if (preOrder != null) {
            preOrder.getItems().stream()
                .sorted(Comparator.comparingLong(PreOrderItem::productId))
                .forEach(item -> productService.incrementStock(item.productId(), item.quantity()));
            restoreCoupon(preOrder);
            log.info("[Payment] 재고+쿠폰 복구 완료: paymentId={}", paymentId);
        } else {
            log.error("[Payment] 재고 복구 실패 - pre-order 복구 불가: preOrderId={}", payment.getPreOrderId());
        }
        payment.fail(reason);
        paymentService.save(payment);
        log.info("[Payment] 결제 실패 처리 완료: paymentId={}, reason={}", paymentId, reason);
    }

    /** 선차감된 쿠폰을 복구한다 (보상 트랜잭션). */
    private void restoreCoupon(PreOrder preOrder) {
        if (preOrder.getIssuedCouponId() == null) {
            return;
        }
        issuedCouponRepository.findById(preOrder.getIssuedCouponId())
            .ifPresent(coupon -> {
                coupon.restore();
                log.info("[SAGA] 쿠폰 복구 완료: issuedCouponId={}", preOrder.getIssuedCouponId());
            });
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
}
