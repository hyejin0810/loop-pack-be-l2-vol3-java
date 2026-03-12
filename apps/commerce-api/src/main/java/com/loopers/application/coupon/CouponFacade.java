package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponTemplateRepository;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponFacade {

    private final CouponTemplateRepository couponTemplateRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final UserService userService;

    // Admin: 쿠폰 템플릿 등록
    @Transactional
    public CouponInfo.TemplateInfo createCouponTemplate(CouponTemplateCommand command) {
        CouponTemplate template = new CouponTemplate(
            command.name(), command.type(), command.value(),
            command.minOrderAmount(), command.expiredAt()
        );
        return CouponInfo.TemplateInfo.from(couponTemplateRepository.save(template));
    }

    // Admin: 쿠폰 템플릿 목록 조회
    @Transactional(readOnly = true)
    public Page<CouponInfo.TemplateInfo> getCouponTemplates(Pageable pageable) {
        return couponTemplateRepository.findAll(pageable)
            .map(CouponInfo.TemplateInfo::from);
    }

    // Admin: 쿠폰 템플릿 단건 조회
    @Transactional(readOnly = true)
    public CouponInfo.TemplateInfo getCouponTemplate(Long couponTemplateId) {
        CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
        return CouponInfo.TemplateInfo.from(template);
    }

    // Admin: 쿠폰 템플릿 수정
    @Transactional
    public CouponInfo.TemplateInfo updateCouponTemplate(Long couponTemplateId, CouponTemplateCommand command) {
        CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
        template.update(command.name(), command.type(), command.value(),
            command.minOrderAmount(), command.expiredAt());
        return CouponInfo.TemplateInfo.from(couponTemplateRepository.save(template));
    }

    // Admin: 쿠폰 템플릿 삭제
    @Transactional
    public void deleteCouponTemplate(Long couponTemplateId) {
        CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
        template.delete();
        couponTemplateRepository.save(template);
    }

    // Admin: 특정 쿠폰의 발급 내역 조회
    @Transactional(readOnly = true)
    public List<CouponInfo.IssuedInfo> getIssuedCoupons(Long couponTemplateId) {
        couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
        return issuedCouponRepository.findAllByCouponTemplateId(couponTemplateId).stream()
            .map(CouponInfo.IssuedInfo::from)
            .toList();
    }

    // User: 쿠폰 발급
    @Transactional
    public CouponInfo.IssuedInfo issueCoupon(String loginId, String rawPassword, Long couponTemplateId) {
        User user = userService.authenticate(loginId, rawPassword);

        CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));

        if (issuedCouponRepository.existsByUserIdAndCouponTemplateId(user.getId(), template.getId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.");
        }

        IssuedCoupon issuedCoupon = issuedCouponRepository.save(new IssuedCoupon(user.getId(), template.getId()));
        return CouponInfo.IssuedInfo.from(issuedCoupon);
    }

    // User: 내 쿠폰 목록 조회
    @Transactional(readOnly = true)
    public List<CouponInfo.IssuedInfo> getMyCoupons(String loginId, String rawPassword) {
        User user = userService.authenticate(loginId, rawPassword);
        return issuedCouponRepository.findAllByUserId(user.getId()).stream()
            .map(CouponInfo.IssuedInfo::from)
            .toList();
    }
}
