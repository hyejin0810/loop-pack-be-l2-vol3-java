package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponTemplateRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private UserService userService;

    private CouponFacade couponFacade;

    private static final ZonedDateTime FUTURE = ZonedDateTime.now().plusDays(30);

    @BeforeEach
    void setUp() {
        couponFacade = new CouponFacade(couponTemplateRepository, issuedCouponRepository, userService);
    }

    @DisplayName("쿠폰 발급")
    @Nested
    class IssueCoupon {

        @DisplayName("유효한 쿠폰 템플릿으로 발급 요청하면, 발급된 쿠폰을 반환한다.")
        @Test
        void returnsIssuedCoupon_whenTemplateIsValid() {
            // Arrange
            String loginId = "user1";
            String rawPassword = "Test1234!";
            User user = new User(loginId, "encoded", "홍길동", "19900101", "test@naver.com");
            CouponTemplate template = new CouponTemplate("10% 할인", CouponType.RATE, 10, null, FUTURE);

            given(userService.authenticate(loginId, rawPassword)).willReturn(user);
            given(couponTemplateRepository.findById(1L)).willReturn(Optional.of(template));
            given(issuedCouponRepository.existsByUserIdAndCouponTemplateId(any(), any())).willReturn(false);
            given(issuedCouponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // Act
            CouponInfo.IssuedInfo result = couponFacade.issueCoupon(loginId, rawPassword, 1L);

            // Assert
            assertThat(result).isNotNull();
        }

        @DisplayName("존재하지 않는 쿠폰 템플릿으로 발급 요청하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenTemplateDoesNotExist() {
            // Arrange
            String loginId = "user1";
            String rawPassword = "Test1234!";
            User user = new User(loginId, "encoded", "홍길동", "19900101", "test@naver.com");

            given(userService.authenticate(loginId, rawPassword)).willReturn(user);
            given(couponTemplateRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> couponFacade.issueCoupon(loginId, rawPassword, 999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("이미 발급받은 쿠폰을 다시 발급 요청하면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenCouponAlreadyIssued() {
            // Arrange
            String loginId = "user1";
            String rawPassword = "Test1234!";
            User user = new User(loginId, "encoded", "홍길동", "19900101", "test@naver.com");
            CouponTemplate template = new CouponTemplate("10% 할인", CouponType.RATE, 10, null, FUTURE);

            given(userService.authenticate(loginId, rawPassword)).willReturn(user);
            given(couponTemplateRepository.findById(1L)).willReturn(Optional.of(template));
            given(issuedCouponRepository.existsByUserIdAndCouponTemplateId(any(), any())).willReturn(true);

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> couponFacade.issueCoupon(loginId, rawPassword, 1L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("내 쿠폰 목록 조회")
    @Nested
    class GetMyCoupons {

        @DisplayName("인증된 유저의 쿠폰 목록을 반환한다.")
        @Test
        void returnsMyCoupons_whenAuthenticated() {
            // Arrange
            String loginId = "user1";
            String rawPassword = "Test1234!";
            User user = new User(loginId, "encoded", "홍길동", "19900101", "test@naver.com");

            given(userService.authenticate(loginId, rawPassword)).willReturn(user);
            given(issuedCouponRepository.findAllByUserId(any())).willReturn(java.util.List.of());

            // Act
            var result = couponFacade.getMyCoupons(loginId, rawPassword);

            // Assert
            assertThat(result).isNotNull();
        }
    }
}
