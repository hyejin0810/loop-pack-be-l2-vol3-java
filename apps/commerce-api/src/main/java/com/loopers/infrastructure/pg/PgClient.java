package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "pgClient", url = "${pg.simulator.url}", configuration = PgClientConfig.class)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PgPaymentRequest request
    );

    @GetMapping("/api/v1/payments/{transactionId}")
    PgStatusResponse getPaymentByTransactionId(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable("transactionId") String transactionId
    );

    @GetMapping("/api/v1/payments")
    List<PgStatusResponse> getPaymentsByOrderId(
        @RequestHeader("X-USER-ID") String userId,
        @RequestParam("orderId") String orderId
    );
}
