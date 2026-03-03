package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponStatus;

import java.time.ZonedDateTime;

public class CouponInfo {

    public record TemplateInfo(
        Long id,
        String name,
        CouponType type,
        Integer value,
        Integer minOrderAmount,
        ZonedDateTime expiredAt
    ) {
        public static TemplateInfo from(CouponTemplate template) {
            return new TemplateInfo(
                template.getId(),
                template.getName(),
                template.getType(),
                template.getValue(),
                template.getMinOrderAmount(),
                template.getExpiredAt()
            );
        }
    }

    public record IssuedInfo(
        Long id,
        Long couponTemplateId,
        IssuedCouponStatus status
    ) {
        public static IssuedInfo from(IssuedCoupon issuedCoupon) {
            return new IssuedInfo(
                issuedCoupon.getId(),
                issuedCoupon.getCouponTemplateId(),
                issuedCoupon.getStatus()
            );
        }
    }
}
