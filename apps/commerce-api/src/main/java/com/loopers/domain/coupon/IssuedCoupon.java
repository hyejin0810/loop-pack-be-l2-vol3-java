package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "issued_coupons")
@Getter
public class IssuedCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IssuedCouponStatus status;

    protected IssuedCoupon() {}

    public IssuedCoupon(Long userId, Long couponTemplateId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId는 필수입니다.");
        }
        if (couponTemplateId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "couponTemplateId는 필수입니다.");
        }
        this.userId = userId;
        this.couponTemplateId = couponTemplateId;
        this.status = IssuedCouponStatus.AVAILABLE;
    }

    public void use() {
        if (this.status != IssuedCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 가능한 쿠폰이 아닙니다.");
        }
        this.status = IssuedCouponStatus.USED;
    }

    public void expire() {
        this.status = IssuedCouponStatus.EXPIRED;
    }
}
