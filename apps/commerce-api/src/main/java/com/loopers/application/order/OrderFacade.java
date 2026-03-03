package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponTemplateRepository;
import com.loopers.domain.coupon.IssuedCoupon;
import com.loopers.domain.coupon.IssuedCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final BrandService brandService;
    private final CouponTemplateRepository couponTemplateRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional
    public OrderInfo createOrder(String loginId, String rawPassword,
                                 List<OrderRequest.OrderItemRequest> items, Long issuedCouponId) {
        User user = userService.authenticate(loginId, rawPassword);

        List<Product> products = new ArrayList<>();
        long originalAmount = 0L;
        for (OrderRequest.OrderItemRequest item : items) {
            Product product = productService.getProduct(item.productId());
            product.decreaseStock(item.quantity());
            originalAmount += (long) product.getPrice() * item.quantity();
            products.add(product);
        }

        // 쿠폰 적용
        long discountAmount = 0L;
        IssuedCoupon issuedCoupon = null;
        if (issuedCouponId != null) {
            issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

            if (!issuedCoupon.getUserId().equals(user.getId())) {
                throw new CoreException(ErrorType.BAD_REQUEST, "본인 소유의 쿠폰이 아닙니다.");
            }

            CouponTemplate template = couponTemplateRepository.findById(issuedCoupon.getCouponTemplateId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));

            // 만료 여부는 CouponTemplate 엔티티가 판단 (도메인 규칙)
            if (template.isExpired()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.");
            }

            discountAmount = template.calculateDiscount(originalAmount);
            issuedCoupon.use();
        }

        long finalAmount = originalAmount - discountAmount;
        user.deductBalance(finalAmount);

        // 주문 시점의 브랜드명을 스냅샷으로 저장하기 위해 브랜드 정보를 한 번에 조회 (N+1 방지)
        List<Long> brandIds = products.stream().map(Product::getBrandId).distinct().toList();
        Map<Long, Brand> brandMap = brandService.getBrandsByIds(brandIds).stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));

        String orderNumber = orderService.generateOrderNumber();
        Order order = issuedCoupon != null
            ? orderService.createOrder(user.getId(), orderNumber, originalAmount, discountAmount, issuedCoupon.getId())
            : orderService.createOrder(user.getId(), orderNumber, originalAmount);

        // 상품명, 브랜드명, 이미지 URL, 단가를 스냅샷으로 저장 (이후 상품 정보 변경에도 주문 내역 보존)
        for (int i = 0; i < items.size(); i++) {
            Product product = products.get(i);
            OrderRequest.OrderItemRequest item = items.get(i);
            Brand brand = brandMap.get(product.getBrandId());
            orderService.createOrderItem(order.getId(), product.getId(),
                product.getName(), brand.getName(), product.getImageUrl(), product.getPrice(), item.quantity());
        }

        List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
        return OrderInfo.from(order, orderItems.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrders(String loginId, String rawPassword, Pageable pageable) {
        User user = userService.authenticate(loginId, rawPassword);
        Page<Order> orders = orderService.getOrders(user.getId(), pageable);

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItemInfo>> itemsByOrderId = orderService.getOrderItemsByOrderIds(orderIds).stream()
            .collect(Collectors.groupingBy(
                OrderItem::getOrderId,
                Collectors.mapping(OrderItemInfo::from, Collectors.toList())
            ));

        return orders.map(order ->
            OrderInfo.from(order, itemsByOrderId.getOrDefault(order.getId(), List.of()))
        );
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderDetail(String loginId, String rawPassword, Long orderId) {
        User user = userService.authenticate(loginId, rawPassword);
        Order order = orderService.getOrder(orderId);
        if (!order.getUserId().equals(user.getId())) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        List<OrderItem> items = orderService.getOrderItems(orderId);
        return OrderInfo.from(order, items.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional
    public OrderInfo cancelOrder(String loginId, String rawPassword, Long orderId) {
        User user = userService.authenticate(loginId, rawPassword);
        Order found = orderService.getOrder(orderId);
        if (!found.getUserId().equals(user.getId())) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        Order order = orderService.cancelOrder(found);

        List<OrderItem> orderItems = orderService.getOrderItems(orderId);
        for (OrderItem item : orderItems) {
            Product product = productService.getProduct(item.getProductId());
            product.increaseStock(item.getQuantity());
        }

        user.restoreBalance(order.getTotalAmount());

        // 쿠폰이 사용된 경우, 템플릿 만료 여부를 확인한 후 복구
        // - 만료되지 않은 쿠폰: USED → AVAILABLE로 복구 (재사용 가능)
        // - 만료된 쿠폰: 복구하지 않음 (어차피 사용 불가)
        if (order.getIssuedCouponId() != null) {
            IssuedCoupon issuedCoupon = issuedCouponRepository.findById(order.getIssuedCouponId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
            CouponTemplate template = couponTemplateRepository.findById(issuedCoupon.getCouponTemplateId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
            if (!template.isExpired()) {
                issuedCoupon.restore();
            }
        }

        return OrderInfo.from(order, orderItems.stream().map(OrderItemInfo::from).toList());
    }

    @Transactional
    public OrderInfo approveOrder(String loginId, String rawPassword, Long orderId) {
        userService.authenticate(loginId, rawPassword);
        Order order = orderService.approveOrder(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);
        return OrderInfo.from(order, items.stream().map(OrderItemInfo::from).toList());
    }
}
