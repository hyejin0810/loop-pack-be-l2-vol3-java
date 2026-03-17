package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPreOrderId(String preOrderId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findPendingBefore(ZonedDateTime before);
}
