package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository);
    }

    @Nested
    @DisplayName("결제 생성")
    class CreatePayment {

        @Test
        @DisplayName("동일한 preOrderId로 결제가 이미 존재하면 CONFLICT 예외가 발생한다.")
        void throwsConflict_whenPaymentAlreadyExists() {
            // Arrange
            String preOrderId = "uuid-001";
            given(paymentRepository.findByPreOrderId(preOrderId))
                .willReturn(Optional.of(new Payment(preOrderId, 2L, "SAMSUNG", "1234-5678", 10000L, null)));

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                paymentService.createPayment(preOrderId, 2L, "SAMSUNG", "1234-5678", 10000L, null)
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @Test
        @DisplayName("유효한 요청이면 PENDING 상태의 결제가 생성된다.")
        void createPayment_withValidRequest_returnsPending() {
            // Arrange
            String preOrderId = "uuid-001";
            given(paymentRepository.findByPreOrderId(preOrderId)).willReturn(Optional.empty());
            given(paymentRepository.save(any(Payment.class)))
                .willAnswer(inv -> inv.getArgument(0));

            // Act
            Payment result = paymentService.createPayment(preOrderId, 2L, "SAMSUNG", "1234-5678", 10000L, null);

            // Assert
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getPreOrderId()).isEqualTo(preOrderId);
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("결제 조회")
    class GetPayment {

        @Test
        @DisplayName("존재하지 않는 paymentId로 조회하면 NOT_FOUND 예외가 발생한다.")
        void throwsNotFound_whenPaymentNotExists() {
            // Arrange
            given(paymentRepository.findById(99L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                paymentService.getPayment(99L)
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 transactionId로 조회하면 NOT_FOUND 예외가 발생한다.")
        void throwsNotFound_whenTransactionNotExists() {
            // Arrange
            given(paymentRepository.findByTransactionId("invalid-txn")).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                paymentService.getPaymentByTransactionId("invalid-txn")
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
