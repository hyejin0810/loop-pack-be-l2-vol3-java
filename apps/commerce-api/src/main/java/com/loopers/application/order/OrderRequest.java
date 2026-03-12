package com.loopers.application.order;

import java.util.List;

public record OrderRequest(
    List<OrderItemRequest> items
) {
    public record OrderItemRequest(
        Long productId,
        Integer quantity
    ) {}
}
