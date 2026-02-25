package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Order createOrder(Long userId, String orderNumber, Long totalAmount) {
        return orderRepository.save(new Order(userId, orderNumber, totalAmount));
    }

    public OrderItem createOrderItem(Long orderId, Long productId, String productName,
                                     String brandName, String imageUrl, Integer price, Integer quantity) {
        return orderItemRepository.save(new OrderItem(orderId, productId, productName, brandName, imageUrl, price, quantity));
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public Page<Order> getOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Order cancelOrder(Order order) {
        order.cancel();
        return orderRepository.save(order);
    }

    public Order approveOrder(Long id) {
        Order order = getOrder(id);
        order.approve();
        return orderRepository.save(order);
    }

    public String generateOrderNumber() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + uuid;
    }
}
