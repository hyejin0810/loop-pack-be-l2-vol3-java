package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandFacade {

    private final BrandService brandService;
    private final ProductService productService;

    @Transactional
    public BrandInfo register(String name, String description) {
        Brand brand = brandService.register(name, description);
        return BrandInfo.from(brand);
    }

    @Transactional(readOnly = true)
    public BrandInfo getBrand(Long id) {
        return BrandInfo.from(brandService.getBrand(id));
    }

    @Transactional(readOnly = true)
    public List<BrandInfo> getBrands() {
        return brandService.getBrands().stream()
            .map(BrandInfo::from)
            .toList();
    }

    @Transactional
    public void deleteBrand(Long id) {
        productService.deleteProductsByBrandId(id);
        brandService.deleteBrand(id);
    }
}
