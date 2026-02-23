package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository);
    }

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @DisplayName("유효한 정보로 생성하면, 주문을 반환한다.")
        @Test
        void returnsOrder_whenInputIsValid() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            given(orderRepository.save(any(Order.class))).willReturn(order);

            // Act
            Order result = orderService.createOrder(1L, "ORD-20240101-ABCD1234", 50000L);

            // Assert
            assertThat(result.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @DisplayName("주문 단건 조회")
    @Nested
    class GetOrder {

        @DisplayName("존재하는 ID로 조회하면, 주문을 반환한다.")
        @Test
        void returnsOrder_whenIdExists() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // Act
            Order result = orderService.getOrder(1L);

            // Assert
            assertThat(result.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenIdDoesNotExist() {
            // Arrange
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> orderService.getOrder(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("주문 목록 조회")
    @Nested
    class GetOrders {

        @DisplayName("userId로 주문 목록을 페이지로 반환한다.")
        @Test
        void returnsOrderPage_byUserId() {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Order> page = new PageImpl<>(List.of(
                new Order(1L, "ORD-20240101-ABCD1234", 50000L)
            ));
            given(orderRepository.findByUserId(1L, pageable)).willReturn(page);

            // Act
            Page<Order> result = orderService.getOrders(1L, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @DisplayName("주문 취소")
    @Nested
    class CancelOrder {

        @DisplayName("PENDING 상태의 주문을 취소하면, CANCELLED 상태가 된다.")
        @Test
        void cancelsOrder_whenStatusIsPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(orderRepository.save(order)).willReturn(order);

            // Act
            Order result = orderService.cancelOrder(1L);

            // Assert
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @DisplayName("존재하지 않는 ID로 취소하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenOrderDoesNotExist() {
            // Arrange
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> orderService.cancelOrder(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("주문 승인")
    @Nested
    class ApproveOrder {

        @DisplayName("PENDING 상태의 주문을 승인하면, CONFIRMED 상태가 된다.")
        @Test
        void approvesOrder_whenStatusIsPending() {
            // Arrange
            Order order = new Order(1L, "ORD-20240101-ABCD1234", 50000L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(orderRepository.save(order)).willReturn(order);

            // Act
            Order result = orderService.approveOrder(1L);

            // Assert
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }
}
