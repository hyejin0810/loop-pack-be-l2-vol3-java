package com.loopers.interfaces.api.like;

import com.loopers.application.product.ProductInfo;

public class LikeV1Dto {

    public record AddLikeRequest(
        Long productId
    ) {}

    public record LikedProductResponse(
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
        public static LikedProductResponse from(ProductInfo info) {
            return new LikedProductResponse(
                info.id(),
                info.brandId(),
                info.brandName(),
                info.name(),
                info.price(),
                info.stock(),
                info.likesCount(),
                info.description(),
                info.imageUrl()
            );
        }
    }
}
