package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public record OrderInfo(
    Long id,
    Long userId,
    String orderNumber,
    OrderStatus status,
    Long originalAmount,
    Long discountAmount,
    Long totalAmount,
    Long issuedCouponId,
    List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order, List<OrderItemInfo> items) {
        return new OrderInfo(
            order.getId(),
            order.getUserId(),
            order.getOrderNumber(),
            order.getStatus(),
            order.getOriginalAmount(),
            order.getDiscountAmount(),
            order.getTotalAmount(),
            order.getIssuedCouponId(),
            items
        );
    }
}
