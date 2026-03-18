package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 콜백 누락 감지 스케줄러
 *
 * 5분마다 실행하여 10분 이상 PENDING 상태인 결제를 PG에 직접 조회,
 * 콜백을 놓친 결제를 자동으로 처리(SAGA 재실행)한다.
 */
@RequiredArgsConstructor
@Component
public class PaymentSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSyncScheduler.class);
    private static final int PENDING_THRESHOLD_MINUTES = 3;

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    @Scheduled(fixedDelay = 60 * 1000)  // 1분마다
    public void syncStalePendingPayments() {
        List<Payment> stalePending = paymentService.getPendingPaymentsOlderThan(PENDING_THRESHOLD_MINUTES);
        if (stalePending.isEmpty()) {
            return;
        }

        log.info("[Scheduler] 콜백 누락 감지: {}건 동기화 시작", stalePending.size());
        for (Payment payment : stalePending) {
            try {
                paymentFacade.syncPaymentById(payment.getId());
            } catch (Exception e) {
                log.warn("[Scheduler] 동기화 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
            }
        }
    }
}
