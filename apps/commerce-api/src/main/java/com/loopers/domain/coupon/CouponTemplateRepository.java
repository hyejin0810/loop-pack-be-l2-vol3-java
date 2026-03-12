package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CouponTemplateRepository {
    CouponTemplate save(CouponTemplate couponTemplate);
    Optional<CouponTemplate> findById(Long id);
    Page<CouponTemplate> findAll(Pageable pageable);
    void deleteById(Long id);
}
