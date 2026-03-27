package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.IssuedCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
    Optional<IssuedCoupon> findByIdAndDeletedAtIsNull(Long id);
    List<IssuedCoupon> findAllByUserIdAndDeletedAtIsNull(Long userId);
    boolean existsByUserIdAndCouponTemplateIdAndDeletedAtIsNull(Long userId, Long couponTemplateId);
    List<IssuedCoupon> findAllByCouponTemplateIdAndDeletedAtIsNull(Long couponTemplateId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT ic FROM IssuedCoupon ic WHERE ic.id = :id AND ic.deletedAt IS NULL")
    Optional<IssuedCoupon> findByIdForUpdateAndDeletedAtIsNull(@Param("id") Long id);
}
