package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponRepository {
    IssuedCoupon save(IssuedCoupon issuedCoupon);
    Optional<IssuedCoupon> findById(Long id);
    Optional<IssuedCoupon> findByIdForUpdate(Long id);
    List<IssuedCoupon> findAllByUserId(Long userId);
    boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId);
    List<IssuedCoupon> findAllByCouponTemplateId(Long couponTemplateId);
}
