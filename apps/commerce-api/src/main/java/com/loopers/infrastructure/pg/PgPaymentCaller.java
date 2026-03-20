package com.loopers.infrastructure.pg;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PG 시스템 호출을 담당하는 컴포넌트.
 * CircuitBreaker와 Retry 어노테이션이 AOP 프록시에서 동작하려면
 * self-call 이 아닌 별도 Spring Bean이어야 한다.
 */
@RequiredArgsConstructor
@Component
public class PgPaymentCaller {

    private static final Logger log = LoggerFactory.getLogger(PgPaymentCaller.class);

    private final PgClient pgClient;

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    public PgPaymentResponse requestPayment(String userId, PgPaymentRequest request) {
        return pgClient.requestPayment(userId, request);
    }

    public PgPaymentResponse requestPaymentFallback(String userId, PgPaymentRequest request, Throwable t) {
        log.warn("[PG] CircuitBreaker/Retry 소진으로 fallback 실행: orderId={}, error={}", request.orderId(), t.getMessage());
        throw new PgUnavailableException("PG 시스템에 접근할 수 없습니다.", t);
    }

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentsByOrderIdFallback")
    public List<PgStatusResponse> getPaymentsByOrderId(String userId, String orderId) {
        return pgClient.getPaymentsByOrderId(userId, orderId);
    }

    public List<PgStatusResponse> getPaymentsByOrderIdFallback(String userId, String orderId, Throwable t) {
        log.warn("[PG] CircuitBreaker open으로 조회 불가: orderId={}, error={}", orderId, t.getMessage());
        return List.of();
    }
}
