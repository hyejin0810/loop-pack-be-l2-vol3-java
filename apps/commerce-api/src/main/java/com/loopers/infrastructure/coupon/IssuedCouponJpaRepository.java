package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
    Optional<IssuedCoupon> findByIdAndDeletedAtIsNull(Long id);
    List<IssuedCoupon> findAllByUserIdAndDeletedAtIsNull(Long userId);
    boolean existsByUserIdAndCouponTemplateIdAndDeletedAtIsNull(Long userId, Long couponTemplateId);
    List<IssuedCoupon> findAllByCouponTemplateIdAndDeletedAtIsNull(Long couponTemplateId);
}
