classDiagram
%% ========================================
%% Entity (도메인 모델)
%% ========================================

    class User {
        -Long id
        -String loginId
        -String password
        -String name
        +login() boolean
        +changePassword() void
    }
    
    class Brand {
        -Long id
        -String name
        -LocalDateTime deletedAt
        +delete() void
        +isDeleted() boolean
    }
    
    class Product {
        -Long id
        -String name
        -Integer price
        -Integer stock
        -Integer version
        -Integer likesCount
        +decreaseStock(quantity) void
        +increaseStock(quantity) void
        +increaseLikes() void
        +decreaseLikes() void
    }
    
    class Like {
        -Long id
        -Long userId
        -Long productId
    }
    
    class Cart {
        -Long id
        -Long userId
        -Long productId
        -Integer quantity
        +updateQuantity(quantity) void
    }
    
    class Order {
        -Long id
        -String orderNumber
        -Integer totalAmount
        -OrderStatus status
        +confirm() void
        +cancel() void
        +canCancel() boolean
    }
    
    class OrderItem {
        -Long id
        -Integer quantity
        -String productName
        -Integer productPrice
    }
    
    class OrderStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        CANCELLED
    }
    
    %% ========================================
    %% Service (비즈니스 로직)
    %% ========================================
    
    class ProductService {
        +create() Product
        +update() Product
        +delete() void
    }
    
    class LikeService {
        +addLike() void
        +removeLike() void
    }
    
    class OrderService {
        +createOrder() Order
        +confirmOrder() void
        +cancelOrder() void
    }
    
    %% ========================================
    %% Repository (데이터 접근)
    %% ========================================
    
    class ProductRepository {
        <<interface>>
        +findById() Optional~Product~
        +save() Product
    }
    
    class LikeRepository {
        <<interface>>
        +save() Like
        +delete() void
    }
    
    class OrderRepository {
        <<interface>>
        +save() Order
        +findById() Optional~Order~
    }
    
    %% ========================================
    %% 관계
    %% ========================================
    
    User "1" --> "*" Like
    User "1" --> "*" Cart
    User "1" --> "*" Order
    
    Brand "1" --> "*" Product
    
    Product "1" --> "*" Like
    Product "1" --> "*" Cart
    Product "1" --> "*" OrderItem
    
    Order "1" *-- "*" OrderItem
    Order --> OrderStatus
    
    ProductService --> ProductRepository
    LikeService --> LikeRepository
    OrderService --> OrderRepository
    
    ProductRepository ..> Product
    LikeRepository ..> Like
    OrderRepository ..> Order




---------------------------------------------------------------------------------------

classDiagram
%% ========================================
%% Entity (도메인 모델)
%% ========================================

    class User {
        -Long id
        -String loginId
        -String password
        -String name
        -String email
        -String phone
        +login() boolean
        +changePassword() void
    }
    
    class Brand {
        -Long id
        -String name
        -String description
        -LocalDateTime deletedAt
        +delete() void
        +isDeleted() boolean
    }
    
    class Product {
        -Long id
        -Long brandId
        -String name
        -Integer price
        -Integer stock
        -Integer version
        -Integer likesCount
        -LocalDateTime deletedAt
        +decreaseStock(quantity) void
        +increaseStock(quantity) void
        +increaseLikes() void
        +decreaseLikes() void
        +isStockAvailable(quantity) boolean
        +isDeleted() boolean
    }
    
    class Like {
        -Long id
        -Long userId
        -Long productId
        -LocalDateTime createdAt
    }
    
    class Cart {
        -Long id
        -Long userId
        -Long productId
        -Integer quantity
        -LocalDateTime createdAt
        +updateQuantity(quantity) void
        +addQuantity(quantity) void
    }
    
    class Order {
        -Long id
        -String orderNumber
        -Long userId
        -Integer totalAmount
        -OrderStatus status
        -LocalDateTime orderedAt
        +confirm() void
        +cancel() void
        +canCancel() boolean
        +isPending() boolean
    }
    
    class OrderItem {
        -Long id
        -Long orderId
        -Long productId
        -Integer quantity
        -String productName
        -Integer productPrice
        -String brandName
        +getTotalPrice() Integer
    }
    
    class OrderStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        CANCELLED
    }
    
    %% ========================================
    %% Service (비즈니스 로직)
    %% ========================================
    
    class UserService {
        +register(request) User
        +login(loginId, password) String
        +changePassword(userId, newPassword) void
    }
    
    class BrandService {
        +create(request) Brand
        +update(brandId, request) Brand
        +delete(brandId) void
        +getBrand(brandId) Brand
    }
    
    class ProductService {
        +create(request) Product
        +update(productId, request) Product
        +delete(productId) void
        +getProduct(productId) Product
    }
    
    class LikeService {
        +addLike(userId, productId) void
        +removeLike(userId, productId) void
        +getMyLikes(userId) List~Like~
    }
    
    class CartService {
        +addToCart(userId, productId, quantity) void
        +updateQuantity(cartId, quantity) void
        +removeFromCart(cartId) void
        +getMyCart(userId) List~Cart~
    }
    
    class OrderService {
        +createOrder(userId, request) Order
        +confirmOrder(orderId) void
        +cancelOrder(orderId) void
        +getOrder(orderId) Order
    }
    
    %% ========================================
    %% Repository (데이터 접근)
    %% ========================================
    
    class UserRepository {
        <<interface>>
        +findById(id) Optional~User~
        +findByLoginId(loginId) Optional~User~
        +save(user) User
    }
    
    class BrandRepository {
        <<interface>>
        +findById(id) Optional~Brand~
        +save(brand) Brand
        +delete(brand) void
    }
    
    class ProductRepository {
        <<interface>>
        +findById(id) Optional~Product~
        +findByIdWithLock(id) Optional~Product~
        +save(product) Product
    }
    
    class LikeRepository {
        <<interface>>
        +findByUserIdAndProductId(userId, productId) Optional~Like~
        +save(like) Like
        +delete(like) void
    }
    
    class CartRepository {
        <<interface>>
        +findByUserIdAndProductId(userId, productId) Optional~Cart~
        +findByUserId(userId) List~Cart~
        +save(cart) Cart
        +delete(cart) void
    }
    
    class OrderRepository {
        <<interface>>
        +findById(id) Optional~Order~
        +findByUserId(userId) List~Order~
        +save(order) Order
    }
    
    class OrderItemRepository {
        <<interface>>
        +findByOrderId(orderId) List~OrderItem~
        +save(orderItem) OrderItem
    }
    
    %% ========================================
    %% 관계
    %% ========================================
    
    %% Entity 관계
    User "1" --> "*" Like : 좋아요
    User "1" --> "*" Cart : 장바구니
    User "1" --> "*" Order : 주문
    
    Brand "1" --> "*" Product : 포함
    
    Product "1" --> "*" Like : 좋아요받음
    Product "1" --> "*" Cart : 담김
    Product "1" --> "*" OrderItem : 주문됨
    
    Order "1" *-- "*" OrderItem : 포함
    Order --> OrderStatus : 상태
    
    %% Service와 Repository 관계
    UserService --> UserRepository : 사용
    BrandService --> BrandRepository : 사용
    BrandService --> ProductRepository : 사용
    ProductService --> ProductRepository : 사용
    ProductService --> BrandRepository : 사용
    LikeService --> LikeRepository : 사용
    LikeService --> ProductRepository : 사용
    CartService --> CartRepository : 사용
    CartService --> ProductRepository : 사용
    OrderService --> OrderRepository : 사용
    OrderService --> OrderItemRepository : 사용
    OrderService --> ProductRepository : 사용
    OrderService --> CartRepository : 사용
    
    %% Repository와 Entity 관계
    UserRepository ..> User : 관리
    BrandRepository ..> Brand : 관리
    ProductRepository ..> Product : 관리
    LikeRepository ..> Like : 관리
    CartRepository ..> Cart : 관리
    OrderRepository ..> Order : 관리
    OrderItemRepository ..> OrderItem : 관리