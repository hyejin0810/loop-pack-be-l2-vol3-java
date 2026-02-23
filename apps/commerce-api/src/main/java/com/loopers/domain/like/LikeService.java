package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public Like addLike(Long userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId).ifPresent(l -> {
            throw new CoreException(ErrorType.CONFLICT, "이미 좋아요를 누른 상품입니다.");
        });
        return likeRepository.save(new Like(userId, productId));
    }

    @Transactional
    public void removeLike(Long userId, Long productId) {
        Like like = likeRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "좋아요를 찾을 수 없습니다."));
        likeRepository.delete(like);
    }
}
