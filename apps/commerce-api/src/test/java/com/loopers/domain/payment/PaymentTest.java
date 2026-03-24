package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Payment 엔티티")
class PaymentTest {

    @Nested
    @DisplayName("결제 생성")
    class Create {

        @Test
        @DisplayName("유효한 정보로 결제를 생성하면 PENDING 상태로 생성된다.")
        void createPayment_withValidInfo_statusIsPending() {
            // Arrange & Act
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getTransactionId()).isNull();
            assertThat(payment.getOrderId()).isNull();
        }
    }

    @Nested
    @DisplayName("결제 처리 시작 (markProcessing)")
    class MarkProcessing {

        @Test
        @DisplayName("PENDING 상태에서 transactionId를 설정하면 성공한다.")
        void markProcessing_fromPending_success() {
            // Arrange
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);

            // Act
            payment.markProcessing("20250816:TR:abc123");

            // Assert
            assertThat(payment.getTransactionId()).isEqualTo("20250816:TR:abc123");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 markProcessing을 호출하면 BAD_REQUEST 예외가 발생한다.")
        void markProcessing_fromNonPending_throwsBadRequest() {
            // Arrange
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);
            payment.timeout();

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                payment.markProcessing("20250816:TR:abc123")
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("SAGA 완료 - Order 연결")
    class LinkOrder {

        @Test
        @DisplayName("complete() 후 linkOrder()를 호출하면 orderId가 설정된다.")
        void linkOrder_afterComplete_setsOrderId() {
            // Arrange
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);
            payment.markProcessing("txn-001");
            payment.complete();

            // Act
            payment.linkOrder(42L);

            // Assert
            assertThat(payment.getOrderId()).isEqualTo(42L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("결제 실패")
    class Fail {

        @Test
        @DisplayName("fail()을 호출하면 FAILED 상태와 실패 사유가 설정된다.")
        void fail_setsStatusAndReason() {
            // Arrange
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);

            // Act
            payment.fail("한도 초과");

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("한도 초과");
        }
    }

    @Nested
    @DisplayName("결제 타임아웃")
    class Timeout {

        @Test
        @DisplayName("timeout()을 호출하면 TIMEOUT 상태가 된다.")
        void timeout_setsStatusToTimeout() {
            // Arrange
            Payment payment = new Payment("uuid-001", 2L, "SAMSUNG", "1234-5678-9012-3456", 10000L, null);

            // Act
            payment.timeout();

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.TIMEOUT);
            assertThat(payment.getFailureReason()).isEqualTo("PG 요청 타임아웃");
        }
    }
}
