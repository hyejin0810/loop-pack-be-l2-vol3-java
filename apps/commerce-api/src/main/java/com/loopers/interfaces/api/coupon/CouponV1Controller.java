package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CouponV1Controller {

    private final CouponFacade couponFacade;

    @PostMapping("/coupons/{couponId}/issue")
    public ApiResponse<CouponV1Dto.IssuedCouponResponse> issueCoupon(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword,
        @PathVariable Long couponId
    ) {
        return ApiResponse.success(
            CouponV1Dto.IssuedCouponResponse.from(couponFacade.issueCoupon(loginId, rawPassword, couponId))
        );
    }

    @GetMapping("/users/me/coupons")
    public ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword
    ) {
        return ApiResponse.success(
            couponFacade.getMyCoupons(loginId, rawPassword).stream()
                .map(CouponV1Dto.IssuedCouponResponse::from)
                .toList()
        );
    }
}
