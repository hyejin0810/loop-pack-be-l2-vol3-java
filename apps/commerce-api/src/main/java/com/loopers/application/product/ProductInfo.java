package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public record ProductInfo(
    Long id,
    Long brandId,
    String brandName,
    String name,
    Integer price,
    Integer stock,
    Integer likesCount,
    String description,
    String imageUrl
) {
    public static ProductInfo from(Product product, Brand brand) {
        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 정보가 없습니다.");
        }
        if (brand == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "브랜드 정보를 찾을 수 없습니다.");
        }
        if (!product.getBrandId().equals(brand.getId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품-브랜드 매핑이 올바르지 않습니다.");
        }
        return new ProductInfo(
            product.getId(),
            product.getBrandId(),
            brand.getName(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getLikesCount(),
            product.getDescription(),
            product.getImageUrl()
        );
    }
}
