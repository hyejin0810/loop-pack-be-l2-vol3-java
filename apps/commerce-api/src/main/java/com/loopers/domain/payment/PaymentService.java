package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(String preOrderId, Long userId, String cardType,
                                 String cardNo, Long amount, String preOrderSnapshot) {
        paymentRepository.findByPreOrderId(preOrderId).ifPresent(p -> {
            throw new CoreException(ErrorType.CONFLICT, "이미 결제 요청이 존재합니다.");
        });
        Payment payment = new Payment(preOrderId, userId, cardType, cardNo, amount, preOrderSnapshot);
        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    public Payment getPaymentByPreOrderId(String preOrderId) {
        return paymentRepository.findByPreOrderId(preOrderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "가주문에 대한 결제 정보를 찾을 수 없습니다."));
    }

    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 트랜잭션을 찾을 수 없습니다."));
    }

    /** 콜백 누락 감지용: 생성 10분 이상 지난 PENDING 결제 목록 */
    public List<Payment> getPendingPaymentsOlderThan(int minutes) {
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(minutes);
        return paymentRepository.findPendingBefore(threshold);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
}
