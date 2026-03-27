package com.loopers.infrastructure.preorder;

import java.time.ZonedDateTime;
import java.util.List;

public class PreOrder {

    private String preOrderId;       // UUID → 최종 Order의 orderNumber
    private Long userId;
    private List<PreOrderItem> items;
    private Long issuedCouponId;
    private Long originalAmount;
    private Long discountAmount;
    private Long totalAmount;
    private PreOrderStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime absoluteDeadline;  // 절대 만료 시각 (생성 기준 30분)

    protected PreOrder() {}

    public PreOrder(String preOrderId, Long userId, List<PreOrderItem> items,
                    Long issuedCouponId, Long originalAmount, Long discountAmount) {
        this.preOrderId = preOrderId;
        this.userId = userId;
        this.items = items;
        this.issuedCouponId = issuedCouponId;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = originalAmount - discountAmount;
        this.status = PreOrderStatus.DRAFT;
        this.createdAt = ZonedDateTime.now();
        this.absoluteDeadline = this.createdAt.plusMinutes(30); // 절대 한도 30분
    }

    public boolean isExpired() {
        return ZonedDateTime.now().isAfter(absoluteDeadline);
    }

    public boolean canModify() {
        return status == PreOrderStatus.DRAFT && !isExpired();
    }

    public void updateItems(List<PreOrderItem> newItems, Long newOriginalAmount,
                            Long newDiscountAmount, Long newIssuedCouponId) {
        this.items = newItems;
        this.issuedCouponId = newIssuedCouponId;
        this.originalAmount = newOriginalAmount;
        this.discountAmount = newDiscountAmount;
        this.totalAmount = newOriginalAmount - newDiscountAmount;
    }

    public void markPaymentRequested() {
        this.status = PreOrderStatus.PAYMENT_REQUESTED;
    }

    // Getters
    public String getPreOrderId() { return preOrderId; }
    public Long getUserId() { return userId; }
    public List<PreOrderItem> getItems() { return items; }
    public Long getIssuedCouponId() { return issuedCouponId; }
    public Long getOriginalAmount() { return originalAmount; }
    public Long getDiscountAmount() { return discountAmount; }
    public Long getTotalAmount() { return totalAmount; }
    public PreOrderStatus getStatus() { return status; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getAbsoluteDeadline() { return absoluteDeadline; }

    // Setters (Jackson 역직렬화용)
    public void setPreOrderId(String preOrderId) { this.preOrderId = preOrderId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setItems(List<PreOrderItem> items) { this.items = items; }
    public void setIssuedCouponId(Long issuedCouponId) { this.issuedCouponId = issuedCouponId; }
    public void setOriginalAmount(Long originalAmount) { this.originalAmount = originalAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(PreOrderStatus status) { this.status = status; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
    public void setAbsoluteDeadline(ZonedDateTime absoluteDeadline) { this.absoluteDeadline = absoluteDeadline; }
}
