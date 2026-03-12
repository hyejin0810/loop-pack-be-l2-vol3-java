package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "order_items")
@Getter
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "brand_name", length = 100)
    private String brandName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    protected OrderItem() {}

    public OrderItem(Long orderId, Long productId, String productName, String brandName,
                     String imageUrl, Integer price, Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.brandName = brandName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
    }
}
