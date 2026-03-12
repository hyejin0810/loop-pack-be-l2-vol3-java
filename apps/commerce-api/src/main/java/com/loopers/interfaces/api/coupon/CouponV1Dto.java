package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.CouponTemplateCommand;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.IssuedCouponStatus;

import java.time.ZonedDateTime;

public class CouponV1Dto {

    // Admin 요청
    public record CreateTemplateRequest(
        String name,
        CouponType type,
        Integer value,
        Integer minOrderAmount,
        ZonedDateTime expiredAt
    ) {
        public CouponTemplateCommand toCommand() {
            return new CouponTemplateCommand(name, type, value, minOrderAmount, expiredAt);
        }
    }

    // Admin 응답
    public record TemplateResponse(
        Long id,
        String name,
        CouponType type,
        Integer value,
        Integer minOrderAmount,
        ZonedDateTime expiredAt
    ) {
        public static TemplateResponse from(CouponInfo.TemplateInfo info) {
            return new TemplateResponse(
                info.id(), info.name(), info.type(),
                info.value(), info.minOrderAmount(), info.expiredAt()
            );
        }
    }

    // User 응답 (발급된 쿠폰)
    public record IssuedCouponResponse(
        Long id,
        Long couponTemplateId,
        IssuedCouponStatus status
    ) {
        public static IssuedCouponResponse from(CouponInfo.IssuedInfo info) {
            return new IssuedCouponResponse(info.id(), info.couponTemplateId(), info.status());
        }
    }
}
