package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(likeRepository);
    }

    @DisplayName("좋아요 추가")
    @Nested
    class AddLike {

        @DisplayName("이미 좋아요를 눌렀으면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenAlreadyLiked() {
            // Arrange
            given(likeRepository.findByUserIdAndProductId(1L, 2L))
                .willReturn(Optional.of(new Like(1L, 2L)));

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> likeService.addLike(1L, 2L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("좋아요를 누르지 않은 상태면, Like를 반환한다.")
        @Test
        void returnsLike_whenNotYetLiked() {
            // Arrange
            Like like = new Like(1L, 2L);
            given(likeRepository.findByUserIdAndProductId(1L, 2L)).willReturn(Optional.empty());
            given(likeRepository.save(any(Like.class))).willReturn(like);

            // Act
            Like result = likeService.addLike(1L, 2L);

            // Assert
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getProductId()).isEqualTo(2L);
        }
    }

    @DisplayName("좋아요 취소")
    @Nested
    class RemoveLike {

        @DisplayName("좋아요가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenLikeDoesNotExist() {
            // Arrange
            given(likeRepository.findByUserIdAndProductId(1L, 2L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> likeService.removeLike(1L, 2L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("좋아요가 존재하면, 좋아요를 삭제한다.")
        @Test
        void deletesLike_whenLikeExists() {
            // Arrange
            Like like = new Like(1L, 2L);
            given(likeRepository.findByUserIdAndProductId(1L, 2L)).willReturn(Optional.of(like));

            // Act & Assert (no exception thrown)
            likeService.removeLike(1L, 2L);
        }
    }
}
