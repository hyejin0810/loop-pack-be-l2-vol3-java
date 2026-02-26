package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/likes")
public class LikeV1Controller {

    private final LikeFacade likeFacade;

    @GetMapping
    public ApiResponse<List<LikeV1Dto.LikedProductResponse>> getLikedProducts(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword
    ) {
        List<ProductInfo> products = likeFacade.getLikedProducts(loginId, rawPassword);
        return ApiResponse.success(products.stream().map(LikeV1Dto.LikedProductResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<Void> addLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword,
        @RequestBody LikeV1Dto.AddLikeRequest request
    ) {
        if (request == null || request.productId() == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "productId는 필수입니다.");
        }
        likeFacade.addLike(loginId, rawPassword, request.productId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword,
        @PathVariable Long productId
    ) {
        likeFacade.removeLike(loginId, rawPassword, productId);
        return ApiResponse.success(null);
    }
}
