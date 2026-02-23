package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandFacade {

    private final BrandService brandService;

    public BrandInfo register(String name, String description) {
        Brand brand = brandService.register(name, description);
        return BrandInfo.from(brand);
    }

    public BrandInfo getBrand(Long id) {
        Brand brand = brandService.getBrand(id);
        return BrandInfo.from(brand);
    }

    public List<BrandInfo> getBrands() {
        return brandService.getBrands().stream()
            .map(BrandInfo::from)
            .toList();
    }

    public void deleteBrand(Long id) {
        brandService.deleteBrand(id);
    }
}
