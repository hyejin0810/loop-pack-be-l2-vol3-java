package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class IssuedCouponRepositoryImpl implements IssuedCouponRepository {

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(issuedCoupon);
    }

    @Override
    public Optional<IssuedCoupon> findById(Long id) {
        return issuedCouponJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<IssuedCoupon> findAllByUserId(Long userId) {
        return issuedCouponJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public boolean existsByUserIdAndCouponTemplateId(Long userId, Long couponTemplateId) {
        return issuedCouponJpaRepository.existsByUserIdAndCouponTemplateIdAndDeletedAtIsNull(userId, couponTemplateId);
    }

    @Override
    public List<IssuedCoupon> findAllByCouponTemplateId(Long couponTemplateId) {
        return issuedCouponJpaRepository.findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId);
    }
}
