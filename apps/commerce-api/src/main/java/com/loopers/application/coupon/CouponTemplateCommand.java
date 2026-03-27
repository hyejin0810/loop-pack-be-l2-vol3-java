package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponType;

import java.time.ZonedDateTime;

public record CouponTemplateCommand(
    String name,
    CouponType type,
    Integer value,
    Integer minOrderAmount,
    ZonedDateTime expiredAt
) {}
