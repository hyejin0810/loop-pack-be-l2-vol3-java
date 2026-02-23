package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @DisplayName("주문 생성")
    @Nested
    class Create {

        @DisplayName("유효한 정보로 생성하면, PENDING 상태의 주문이 반환된다.")
        @Test
        void returnsOrder_withPendingStatus() {
            // Act
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);

            // Assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
            assertThat(order.getTotalAmount()).isEqualTo(50000L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @DisplayName("totalAmount가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTotalAmountIsNotPositive() {
            // Act & Assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new Order(1L, "ORD-20240101-ABCD1234", 0L));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("주문 승인")
    @Nested
    class Approve {

        @DisplayName("PENDING 상태의 주문을 승인하면, CONFIRMED 상태가 된다.")
        @Test
        void confirmsOrder_whenStatusIsPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);

            // Act
            order.approve();

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @DisplayName("PENDING이 아닌 주문을 승인하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderIsNotPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            order.approve();

            // Act & Assert
            CoreException exception = assertThrows(CoreException.class, order::approve);
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("주문 취소")
    @Nested
    class Cancel {

        @DisplayName("PENDING 상태의 주문을 취소하면, CANCELLED 상태가 된다.")
        @Test
        void cancelsOrder_whenStatusIsPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);

            // Act
            order.cancel();

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("PENDING이 아닌 주문을 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderIsNotPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            order.cancel();

            // Act & Assert
            CoreException exception = assertThrows(CoreException.class, order::cancel);
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
