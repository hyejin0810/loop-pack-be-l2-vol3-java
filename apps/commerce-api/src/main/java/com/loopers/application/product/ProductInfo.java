package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

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
