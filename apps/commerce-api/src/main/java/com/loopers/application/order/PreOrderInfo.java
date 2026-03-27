package com.loopers.application.order;

import com.loopers.infrastructure.preorder.PreOrder;
import com.loopers.infrastructure.preorder.PreOrderItem;
import com.loopers.infrastructure.preorder.PreOrderStatus;

import java.time.ZonedDateTime;
import java.util.List;

public record PreOrderInfo(
    String preOrderId,
    Long userId,
    List<PreOrderItem> items,
    Long issuedCouponId,
    Long originalAmount,
    Long discountAmount,
    Long totalAmount,
    PreOrderStatus status,
    ZonedDateTime absoluteDeadline
) {
    public static PreOrderInfo from(PreOrder preOrder) {
        return new PreOrderInfo(
            preOrder.getPreOrderId(),
            preOrder.getUserId(),
            preOrder.getItems(),
            preOrder.getIssuedCouponId(),
            preOrder.getOriginalAmount(),
            preOrder.getDiscountAmount(),
            preOrder.getTotalAmount(),
            preOrder.getStatus(),
            preOrder.getAbsoluteDeadline()
        );
    }
}
