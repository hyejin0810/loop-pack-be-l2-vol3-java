package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @Transactional
    public OrderInfo createOrder(String loginId, String rawPassword,
                                 List<OrderRequest.OrderItemRequest> items) {
        User user = userService.authenticate(loginId, rawPassword);

        List<Product> products = new ArrayList<>();
        long totalAmount = 0L;
        for (OrderRequest.OrderItemRequest item : items) {
            Product product = productService.getProduct(item.productId());
            product.decreaseStock(item.quantity());
            totalAmount += (long) product.getPrice() * item.quantity();
            products.add(product);
        }

        user.deductBalance(totalAmount);

        String orderNumber = orderService.generateOrderNumber();
        Order order = orderService.createOrder(user.getId(), orderNumber, totalAmount);

        for (int i = 0; i < items.size(); i++) {
            Product product = products.get(i);
            OrderRequest.OrderItemRequest item = items.get(i);
            orderService.createOrderItem(order.getId(), product.getId(),
                product.getName(), product.getPrice(), item.quantity());
        }

        List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
        return OrderInfo.from(order, orderItems.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrders(String loginId, String rawPassword, Pageable pageable) {
        User user = userService.authenticate(loginId, rawPassword);
        Page<Order> orders = orderService.getOrders(user.getId(), pageable);
        return orders.map(order -> {
            List<OrderItem> items = orderService.getOrderItems(order.getId());
            return OrderInfo.from(order, items.stream().map(OrderItemInfo::from).toList());
        });
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderDetail(String loginId, String rawPassword, Long orderId) {
        userService.authenticate(loginId, rawPassword);
        Order order = orderService.getOrder(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);
        return OrderInfo.from(order, items.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional
    public OrderInfo cancelOrder(String loginId, String rawPassword, Long orderId) {
        User user = userService.authenticate(loginId, rawPassword);
        Order order = orderService.cancelOrder(orderId);

        List<OrderItem> orderItems = orderService.getOrderItems(orderId);
        for (OrderItem item : orderItems) {
            Product product = productService.getProduct(item.getProductId());
            product.increaseStock(item.getQuantity());
        }

        user.restoreBalance(order.getTotalAmount());

        return OrderInfo.from(order, orderItems.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional
    public OrderInfo approveOrder(Long orderId) {
        Order order = orderService.approveOrder(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);
        return OrderInfo.from(order, items.stream().map(OrderItemInfo::from).toList());
    }
}
