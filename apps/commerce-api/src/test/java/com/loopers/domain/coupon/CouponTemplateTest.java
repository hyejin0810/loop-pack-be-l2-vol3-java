package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponTemplateTest {

    private static final ZonedDateTime FUTURE = ZonedDateTime.now().plusDays(30);

    @DisplayName("쿠폰 템플릿 생성")
    @Nested
    class Create {

        @DisplayName("이름이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate(null, CouponType.FIXED, 1000, null, FUTURE));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이름이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate("  ", CouponType.FIXED, 1000, null, FUTURE));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("타입이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTypeIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate("10% 할인", null, 10, null, FUTURE));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("할인값이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsZeroOrNegative() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate("10% 할인", CouponType.RATE, 0, null, FUTURE));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정률 타입에서 할인값이 100 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenRateValueExceeds100() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate("할인", CouponType.RATE, 101, null, FUTURE));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("만료일이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenExpiredAtIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new CouponTemplate("10% 할인", CouponType.RATE, 10, null, null));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("유효한 정액 쿠폰 템플릿을 생성하면, 템플릿이 생성된다.")
        @Test
        void createsCouponTemplate_whenFixedTypeIsValid() {
            CouponTemplate template = new CouponTemplate("1000원 할인", CouponType.FIXED, 1000, 10000, FUTURE);

            assertThat(template.getName()).isEqualTo("1000원 할인");
            assertThat(template.getType()).isEqualTo(CouponType.FIXED);
            assertThat(template.getValue()).isEqualTo(1000);
            assertThat(template.getMinOrderAmount()).isEqualTo(10000);
            assertThat(template.getExpiredAt()).isEqualTo(FUTURE);
        }

        @DisplayName("유효한 정률 쿠폰 템플릿을 생성하면, 템플릿이 생성된다.")
        @Test
        void createsCouponTemplate_whenRateTypeIsValid() {
            CouponTemplate template = new CouponTemplate("10% 할인", CouponType.RATE, 10, null, FUTURE);

            assertThat(template.getName()).isEqualTo("10% 할인");
            assertThat(template.getType()).isEqualTo(CouponType.RATE);
            assertThat(template.getValue()).isEqualTo(10);
            assertThat(template.getMinOrderAmount()).isNull();
        }
    }

    @DisplayName("할인 금액 계산")
    @Nested
    class CalculateDiscount {

        @DisplayName("정액 쿠폰은 고정 금액을 할인한다.")
        @Test
        void calculatesFixedDiscount() {
            CouponTemplate template = new CouponTemplate("1000원 할인", CouponType.FIXED, 1000, null, FUTURE);

            long discount = template.calculateDiscount(50000L);

            assertThat(discount).isEqualTo(1000L);
        }

        @DisplayName("정률 쿠폰은 주문 금액의 비율만큼 할인한다.")
        @Test
        void calculatesRateDiscount() {
            CouponTemplate template = new CouponTemplate("10% 할인", CouponType.RATE, 10, null, FUTURE);

            long discount = template.calculateDiscount(50000L);

            assertThat(discount).isEqualTo(5000L);
        }

        @DisplayName("최소 주문 금액 미달 시, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderAmountIsBelowMinimum() {
            CouponTemplate template = new CouponTemplate("1000원 할인", CouponType.FIXED, 1000, 10000, FUTURE);

            CoreException exception = assertThrows(CoreException.class,
                () -> template.calculateDiscount(5000L));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
