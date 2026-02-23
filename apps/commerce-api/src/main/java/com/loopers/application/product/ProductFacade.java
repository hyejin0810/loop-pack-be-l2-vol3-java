package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;

    public ProductInfo createProduct(Long brandId, String name, Integer price, Integer stock,
                                     String description, String imageUrl) {
        Brand brand = brandService.getBrand(brandId);
        Product product = productService.createProduct(brandId, name, price, stock, description, imageUrl);
        return ProductInfo.from(product, brand);
    }

    public ProductInfo getProductDetail(Long id) {
        Product product = productService.getProduct(id);
        Brand brand = brandService.getBrand(product.getBrandId());
        return ProductInfo.from(product, brand);
    }

    public Page<ProductInfo> getProducts(Long brandId, String sort, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), resolveSort(sort)
        );
        Page<Product> products = productService.getProducts(brandId, sortedPageable);

        List<Long> brandIds = products.stream().map(Product::getBrandId).distinct().toList();
        Map<Long, Brand> brandMap = brandService.getBrandsByIds(brandIds).stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));

        return products.map(p -> ProductInfo.from(p, brandMap.get(p.getBrandId())));
    }

    public ProductInfo updateProduct(Long id, String name, Integer price, Integer stock,
                                     String description, String imageUrl) {
        Product product = productService.updateProduct(id, name, price, stock, description, imageUrl);
        Brand brand = brandService.getBrand(product.getBrandId());
        return ProductInfo.from(product, brand);
    }

    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likesCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
