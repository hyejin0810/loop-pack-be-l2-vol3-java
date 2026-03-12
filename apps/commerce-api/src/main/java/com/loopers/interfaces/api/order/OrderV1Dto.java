package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.application.order.OrderRequest;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public class OrderV1Dto {

    public record CreateOrderRequest(
        List<OrderItemRequest> items,
        Long couponId
    ) {
        public record OrderItemRequest(
            Long productId,
            Integer quantity
        ) {}

        public List<OrderRequest.OrderItemRequest> toOrderItemRequests() {
            return items.stream()
                .map(i -> new OrderRequest.OrderItemRequest(i.productId(), i.quantity()))
                .toList();
        }
    }

    public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer price,
        Integer quantity
    ) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                info.id(),
                info.productId(),
                info.productName(),
                info.price(),
                info.quantity()
            );
        }
    }

    public record OrderResponse(
        Long id,
        Long userId,
        String orderNumber,
        OrderStatus status,
        Long originalAmount,
        Long discountAmount,
        Long totalAmount,
        Long issuedCouponId,
        List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                info.id(),
                info.userId(),
                info.orderNumber(),
                info.status(),
                info.originalAmount(),
                info.discountAmount(),
                info.totalAmount(),
                info.issuedCouponId(),
                info.items().stream().map(OrderItemResponse::from).toList()
            );
        }
    }
}
