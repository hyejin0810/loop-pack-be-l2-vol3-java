package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller {

    private final BrandFacade brandFacade;

    @PostMapping
    public ApiResponse<BrandV1Dto.BrandResponse> register(
        @RequestBody BrandV1Dto.CreateRequest request
    ) {
        BrandInfo info = brandFacade.register(request.name(), request.description());
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(info));
    }

    @GetMapping
    public ApiResponse<List<BrandV1Dto.BrandResponse>> getBrands() {
        List<BrandInfo> infos = brandFacade.getBrands();
        return ApiResponse.success(infos.stream().map(BrandV1Dto.BrandResponse::from).toList());
    }

    @GetMapping("/{brandId}")
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(@PathVariable Long brandId) {
        BrandInfo info = brandFacade.getBrand(brandId);
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(info));
    }

    @DeleteMapping("/{brandId}")
    public ApiResponse<Void> deleteBrand(@PathVariable Long brandId) {
        brandFacade.deleteBrand(brandId);
        return ApiResponse.success(null);
    }
}
