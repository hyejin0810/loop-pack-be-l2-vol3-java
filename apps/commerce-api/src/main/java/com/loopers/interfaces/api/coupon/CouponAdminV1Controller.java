package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/coupons")
public class CouponAdminV1Controller {

    private final CouponFacade couponFacade;

    @PostMapping
    public ApiResponse<CouponV1Dto.TemplateResponse> createCouponTemplate(
        @RequestBody CouponV1Dto.CreateTemplateRequest request
    ) {
        return ApiResponse.success(
            CouponV1Dto.TemplateResponse.from(couponFacade.createCouponTemplate(request.toCommand()))
        );
    }

    @GetMapping
    public ApiResponse<Page<CouponV1Dto.TemplateResponse>> getCouponTemplates(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
            couponFacade.getCouponTemplates(pageable).map(CouponV1Dto.TemplateResponse::from)
        );
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponV1Dto.TemplateResponse> getCouponTemplate(
        @PathVariable Long couponId
    ) {
        return ApiResponse.success(
            CouponV1Dto.TemplateResponse.from(couponFacade.getCouponTemplate(couponId))
        );
    }

    @PutMapping("/{couponId}")
    public ApiResponse<CouponV1Dto.TemplateResponse> updateCouponTemplate(
        @PathVariable Long couponId,
        @RequestBody CouponV1Dto.CreateTemplateRequest request
    ) {
        return ApiResponse.success(
            CouponV1Dto.TemplateResponse.from(couponFacade.updateCouponTemplate(couponId, request.toCommand()))
        );
    }

    @DeleteMapping("/{couponId}")
    public ApiResponse<Void> deleteCouponTemplate(@PathVariable Long couponId) {
        couponFacade.deleteCouponTemplate(couponId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{couponId}/issues")
    public ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> getIssuedCoupons(
        @PathVariable Long couponId
    ) {
        return ApiResponse.success(
            couponFacade.getIssuedCoupons(couponId).stream()
                .map(CouponV1Dto.IssuedCouponResponse::from)
                .toList()
        );
    }
}
