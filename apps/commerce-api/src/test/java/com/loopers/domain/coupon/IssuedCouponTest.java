package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssuedCouponTest {

    @DisplayName("발급 쿠폰 생성")
    @Nested
    class Create {

        @DisplayName("유효한 정보로 생성하면, AVAILABLE 상태로 생성된다.")
        @Test
        void createsIssuedCoupon_withAvailableStatus() {
            IssuedCoupon coupon = new IssuedCoupon(1L, 10L);

            assertThat(coupon.getUserId()).isEqualTo(1L);
            assertThat(coupon.getCouponTemplateId()).isEqualTo(10L);
            assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new IssuedCoupon(null, 10L));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("couponTemplateId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponTemplateIdIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new IssuedCoupon(1L, null));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰 사용")
    @Nested
    class Use {

        @DisplayName("AVAILABLE 상태의 쿠폰을 사용하면, USED 상태로 변경된다.")
        @Test
        void useCoupon_whenStatusIsAvailable() {
            IssuedCoupon coupon = new IssuedCoupon(1L, 10L);

            coupon.use();

            assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.USED);
        }

        @DisplayName("이미 사용된 쿠폰을 사용하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponAlreadyUsed() {
            IssuedCoupon coupon = new IssuedCoupon(1L, 10L);
            coupon.use();

            CoreException exception = assertThrows(CoreException.class, coupon::use);

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("EXPIRED 상태의 쿠폰을 사용하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponIsExpired() {
            IssuedCoupon coupon = new IssuedCoupon(1L, 10L);
            coupon.expire();

            CoreException exception = assertThrows(CoreException.class, coupon::use);

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
