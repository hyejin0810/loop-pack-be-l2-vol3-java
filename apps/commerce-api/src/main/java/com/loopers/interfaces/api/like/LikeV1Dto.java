package com.loopers.interfaces.api.like;

public class LikeV1Dto {

    public record AddLikeRequest(
        Long productId
    ) {}
}
