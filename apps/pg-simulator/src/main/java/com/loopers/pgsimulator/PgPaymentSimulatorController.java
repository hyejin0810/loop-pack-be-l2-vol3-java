package com.loopers.pgsimulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
public class PgPaymentSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(PgPaymentSimulatorController.class);

    private final Random random = new Random();
    private final Map<String, PgPaymentRecord> paymentStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostMapping
    public ResponseEntity<?> requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PgPaymentRequest request
    ) throws InterruptedException {
        // 요청 지연 시뮬레이션: 100ms ~ 500ms
        int requestDelay = random.nextInt(401) + 100;
        Thread.sleep(requestDelay);

        // 요청 성공 확률 60%
        if (random.nextInt(100) >= 60) {
            log.info("[PG] Payment request rejected for orderId={}", request.orderId());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "PG 시스템이 일시적으로 요청을 처리할 수 없습니다."));
        }

        String transactionId = generateTransactionId();
        PgPaymentRecord record = new PgPaymentRecord(
            transactionId, request.orderId(), userId,
            request.cardType(), request.cardNo(), request.amount(), request.callbackUrl()
        );
        paymentStore.put(transactionId, record);

        // 비동기 처리: 1s ~ 5s 지연 후 결제 결과 결정 및 콜백 전송
        int processingDelay = random.nextInt(4001) + 1000;
        scheduler.schedule(() -> processPaymentAsync(record), processingDelay, TimeUnit.MILLISECONDS);

        log.info("[PG] Payment request accepted: transactionId={}, orderId={}", transactionId, request.orderId());

        return ResponseEntity.ok(Map.of(
            "transactionId", transactionId,
            "status", "PROCESSING"
        ));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getPaymentByTransactionId(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable String transactionId
    ) {
        PgPaymentRecord record = paymentStore.get(transactionId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponseMap(record));
    }

    @GetMapping
    public ResponseEntity<?> getPaymentsByOrderId(
        @RequestHeader("X-USER-ID") String userId,
        @RequestParam String orderId
    ) {
        List<Map<String, Object>> results = paymentStore.values().stream()
            .filter(r -> r.getOrderId().equals(orderId))
            .map(this::toResponseMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    private void processPaymentAsync(PgPaymentRecord record) {
        // 처리 결과: 성공 70%, 한도 초과 20%, 잘못된 카드 10%
        int result = random.nextInt(100);
        if (result < 70) {
            record.setStatus(PgPaymentRecord.PgPaymentStatus.SUCCESS);
        } else if (result < 90) {
            record.setStatus(PgPaymentRecord.PgPaymentStatus.LIMIT_EXCEEDED);
            record.setFailureReason("한도 초과");
        } else {
            record.setStatus(PgPaymentRecord.PgPaymentStatus.INVALID_CARD);
            record.setFailureReason("잘못된 카드 정보");
        }

        log.info("[PG] Payment processed: transactionId={}, status={}", record.getTransactionId(), record.getStatus());

        if (record.getCallbackUrl() != null && !record.getCallbackUrl().isBlank()) {
            sendCallback(record);
        }
    }

    private void sendCallback(PgPaymentRecord record) {
        try {
            Map<String, Object> payload = toResponseMap(record);
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(record.getCallbackUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[PG] Callback sent to {}: transactionId={}", record.getCallbackUrl(), record.getTransactionId());
        } catch (Exception e) {
            log.warn("[PG] Failed to send callback: transactionId={}, error={}", record.getTransactionId(), e.getMessage());
        }
    }

    private String generateTransactionId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return date + ":TR:" + suffix;
    }

    private Map<String, Object> toResponseMap(PgPaymentRecord record) {
        return Map.of(
            "transactionId", record.getTransactionId(),
            "orderId", record.getOrderId(),
            "cardType", record.getCardType(),
            "amount", record.getAmount(),
            "status", record.getStatus().name(),
            "failureReason", record.getFailureReason() != null ? record.getFailureReason() : ""
        );
    }
}
