package com.loopers.infrastructure.preorder;

public record PreOrderItem(
    Long productId,
    int quantity,
    int price,
    String productName,
    String brandName,
    String imageUrl
) {}
