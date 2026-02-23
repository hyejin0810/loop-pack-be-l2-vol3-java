package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final LikeService likeService;
    private final ProductService productService;
    private final UserService userService;

    @Transactional
    public void addLike(String loginId, String rawPassword, Long productId) {
        User user = userService.authenticate(loginId, rawPassword);
        Product product = productService.getProduct(productId);
        likeService.addLike(user.getId(), productId);
        product.increaseLikes();
    }

    @Transactional
    public void removeLike(String loginId, String rawPassword, Long productId) {
        User user = userService.authenticate(loginId, rawPassword);
        Product product = productService.getProduct(productId);
        likeService.removeLike(user.getId(), productId);
        product.decreaseLikes();
    }
}
