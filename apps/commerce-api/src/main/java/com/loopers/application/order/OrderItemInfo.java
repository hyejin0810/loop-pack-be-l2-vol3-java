package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(
    Long id,
    Long productId,
    String productName,
    Integer price,
    Integer quantity
) {
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getPrice(),
            item.getQuantity()
        );
    }
}
