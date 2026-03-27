package com.loopers.application.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.pg.PgCallbackPayload;
import com.loopers.infrastructure.pg.PgPaymentCaller;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgUnavailableException;
import com.loopers.infrastructure.preorder.PreOrder;
import com.loopers.infrastructure.preorder.PreOrderCacheService;
import com.loopers.infrastructure.preorder.PreOrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentFacade 단위 테스트")
class PaymentFacadeTest {

    @Mock private PaymentService paymentService;
    @Mock private UserService userService;
    @Mock private ProductService productService;
    @Mock private IssuedCouponRepository issuedCouponRepository;
    @Mock private PreOrderCacheService preOrderCacheService;
    @Mock private PgPaymentCaller pgPaymentCaller;
    @Mock private PaymentSagaService paymentSagaService;

    private PaymentFacade paymentFacade;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        paymentFacade = new PaymentFacade(
            paymentService, userService, productService, issuedCouponRepository,
            preOrderCacheService, pgPaymentCaller, objectMapper, paymentSagaService
        );
        Field callbackUrl = PaymentFacade.class.getDeclaredField("callbackUrl");
        callbackUrl.setAccessible(true);
        callbackUrl.set(paymentFacade, "http://localhost:8080/api/v1/payments/callback");
    }

    @Nested
    @DisplayName("결제 요청 (requestPayment)")
    class RequestPayment {

        private static final String LOGIN_ID = "user1";
        private static final String RAW_PW = "Test1234!";
        private static final String PRE_ORDER_ID = "pre-order-uuid";

        @Test
        @DisplayName("재고가 부족하면 CoreException(BAD_REQUEST)을 던지고 결제를 생성하지 않는다.")
        void throwsBadRequest_whenStockIsInsufficient() {
            // Arrange
            User user = new User(LOGIN_ID, "encoded", "홍길동", "19900101", "test@test.com");
            PreOrderItem item = new PreOrderItem(1L, 3, 5000, "상품A", "브랜드A", "http://img.jpg");
            PreOrder preOrder = new PreOrder(PRE_ORDER_ID, 0L, List.of(item), null, 15000L, 0L);

            given(userService.authenticate(LOGIN_ID, RAW_PW)).willReturn(user);
            given(preOrderCacheService.get(PRE_ORDER_ID)).willReturn(Optional.of(preOrder));
            willThrow(new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다."))
                .given(productService).decrementStock(1L, 3);

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                paymentFacade.requestPayment(LOGIN_ID, RAW_PW, PRE_ORDER_ID, "SAMSUNG", "1234-5678")
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            verify(paymentService, never()).createPayment(anyString(), anyLong(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("재고가 충분하면 재고를 먼저 차감한 후 PG에 결제를 요청한다.")
        void deductsStockBeforePgRequest_whenStockIsSufficient() throws Exception {
            // Arrange
            User user = new User(LOGIN_ID, "encoded", "홍길동", "19900101", "test@test.com");
            PreOrderItem item = new PreOrderItem(1L, 2, 5000, "상품A", "브랜드A", "http://img.jpg");
            PreOrder preOrder = new PreOrder(PRE_ORDER_ID, 0L, List.of(item), null, 10000L, 0L);
            Payment payment = new Payment(PRE_ORDER_ID, 1L, "SAMSUNG", "1234-5678", 10000L, null);

            given(userService.authenticate(LOGIN_ID, RAW_PW)).willReturn(user);
            given(preOrderCacheService.get(PRE_ORDER_ID)).willReturn(Optional.of(preOrder));
            given(paymentService.createPayment(any(), any(), any(), any(), any(), any())).willReturn(payment);
            given(pgPaymentCaller.requestPayment(anyString(), any()))
                .willReturn(new PgPaymentResponse("txn-001", "ACCEPTED"));
            given(paymentService.save(any())).willReturn(payment);

            // Act
            paymentFacade.requestPayment(LOGIN_ID, RAW_PW, PRE_ORDER_ID, "SAMSUNG", "1234-5678");

            // Assert
            verify(productService).decrementStock(1L, 2);
            verify(pgPaymentCaller).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("쿠폰이 있으면 PG 결제 전에 쿠폰을 먼저 사용 처리한다.")
        void usesCouponBeforePgRequest_whenCouponExists() throws Exception {
            // Arrange
            User user = new User(LOGIN_ID, "encoded", "홍길동", "19900101", "test@test.com");
            PreOrderItem item = new PreOrderItem(1L, 2, 5000, "상품A", "브랜드A", "http://img.jpg");
            PreOrder preOrder = new PreOrder(PRE_ORDER_ID, 0L, List.of(item), 10L, 10000L, 1000L);
            Payment payment = new Payment(PRE_ORDER_ID, 1L, "SAMSUNG", "1234-5678", 9000L, null);
            IssuedCoupon coupon = new IssuedCoupon(0L, 1L);

            given(userService.authenticate(LOGIN_ID, RAW_PW)).willReturn(user);
            given(preOrderCacheService.get(PRE_ORDER_ID)).willReturn(Optional.of(preOrder));
            given(issuedCouponRepository.findByIdForUpdate(10L)).willReturn(Optional.of(coupon));
            given(paymentService.createPayment(any(), any(), any(), any(), any(), any())).willReturn(payment);
            given(pgPaymentCaller.requestPayment(anyString(), any()))
                .willReturn(new PgPaymentResponse("txn-001", "ACCEPTED"));
            given(paymentService.save(any())).willReturn(payment);

            // Act
            paymentFacade.requestPayment(LOGIN_ID, RAW_PW, PRE_ORDER_ID, "SAMSUNG", "1234-5678");

            // Assert: 쿠폰이 PG 호출 전에 사용됨
            verify(issuedCouponRepository).findByIdForUpdate(10L);
        }

        @Test
        @DisplayName("PG 호출이 실패(회로 차단)하면 재고를 복구하지 않고 TIMEOUT 상태로 결제를 저장한다.")
        void keepStockDeducted_whenPgCircuitIsOpen() throws Exception {
            // Arrange
            User user = new User(LOGIN_ID, "encoded", "홍길동", "19900101", "test@test.com");
            PreOrderItem item = new PreOrderItem(1L, 2, 5000, "상품A", "브랜드A", "http://img.jpg");
            PreOrder preOrder = new PreOrder(PRE_ORDER_ID, 0L, List.of(item), null, 10000L, 0L);
            Payment payment = new Payment(PRE_ORDER_ID, 1L, "SAMSUNG", "1234-5678", 10000L, null);

            given(userService.authenticate(LOGIN_ID, RAW_PW)).willReturn(user);
            given(preOrderCacheService.get(PRE_ORDER_ID)).willReturn(Optional.of(preOrder));
            given(paymentService.createPayment(any(), any(), any(), any(), any(), any())).willReturn(payment);
            given(pgPaymentCaller.requestPayment(anyString(), any())).willThrow(new PgUnavailableException("circuit open", null));
            given(paymentService.save(any())).willReturn(payment);

            // Act
            paymentFacade.requestPayment(LOGIN_ID, RAW_PW, PRE_ORDER_ID, "SAMSUNG", "1234-5678");

            // Assert: 재고는 복구하지 않는다 (스케줄러가 나중에 처리)
            verify(productService, never()).incrementStock(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("PG 콜백 수신 (handleCallback)")
    class HandleCallback {

        @Test
        @DisplayName("SUCCESS 콜백 수신 시 PaymentSagaService.executeOnSuccess()를 호출한다.")
        void delegatesToSagaService_whenCallbackIsSuccess() {
            // Arrange
            String transactionId = "txn-001";
            Payment payment = new Payment("pre-order-uuid", 1L, "SAMSUNG", "1234-5678", 10000L, null);
            payment.markProcessing(transactionId);

            given(paymentService.getPaymentByTransactionId(transactionId)).willReturn(payment);

            PgCallbackPayload payload = new PgCallbackPayload(transactionId, "pre-order-uuid", "SUCCESS", null);

            // Act
            paymentFacade.handleCallback(payload);

            // Assert
            verify(paymentSagaService).executeOnSuccess(payment.getId());
        }

        @Test
        @DisplayName("LIMIT_EXCEEDED 콜백 수신 시 PaymentSagaService.executeOnFailure()를 호출한다.")
        void delegatesToSagaService_whenCallbackIsLimitExceeded() {
            // Arrange
            String transactionId = "txn-002";
            Payment payment = new Payment("pre-order-uuid", 1L, "SAMSUNG", "1234-5678", 10000L, null);
            payment.markProcessing(transactionId);

            given(paymentService.getPaymentByTransactionId(transactionId)).willReturn(payment);

            PgCallbackPayload payload = new PgCallbackPayload(transactionId, "pre-order-uuid", "LIMIT_EXCEEDED", "한도 초과");

            // Act
            paymentFacade.handleCallback(payload);

            // Assert
            verify(paymentSagaService).executeOnFailure(payment.getId(), "한도 초과");
        }

        @Test
        @DisplayName("이미 완료된 결제의 콜백이 재수신되면 SAGA를 실행하지 않는다. (멱등성)")
        void ignoresDuplicateCallback_whenPaymentIsAlreadyFinalized() {
            // Arrange
            String transactionId = "txn-003";
            Payment payment = new Payment("pre-order-uuid", 1L, "SAMSUNG", "1234-5678", 10000L, null);
            payment.markProcessing(transactionId);
            payment.complete();  // 이미 완료된 상태

            given(paymentService.getPaymentByTransactionId(transactionId)).willReturn(payment);

            PgCallbackPayload payload = new PgCallbackPayload(transactionId, "pre-order-uuid", "SUCCESS", null);

            // Act
            paymentFacade.handleCallback(payload);

            // Assert: SAGA 실행 안 함
            verify(paymentSagaService, never()).executeOnSuccess(anyLong());
            verify(paymentSagaService, never()).executeOnFailure(anyLong(), anyString());
        }
    }
}
