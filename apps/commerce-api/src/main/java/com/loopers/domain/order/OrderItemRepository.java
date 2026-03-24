package com.loopers.domain.order;

import java.util.List;

public interface OrderItemRepository {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIds(List<Long> orderIds);

    OrderItem save(OrderItem orderItem);
}
