package com.loopers.domain.order;

import java.util.List;

public interface OrderItemRepository {

    List<OrderItem> findByOrderId(Long orderId);

    OrderItem save(OrderItem orderItem);
}
