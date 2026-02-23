package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public record OrderInfo(
    Long id,
    Long userId,
    String orderNumber,
    OrderStatus status,
    Long totalAmount,
    List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order, List<OrderItemInfo> items) {
        return new OrderInfo(
            order.getId(),
            order.getUserId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getTotalAmount(),
            items
        );
    }
}
