package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LikeTest {

    @DisplayName("좋아요 생성")
    @Nested
    class Create {

        @DisplayName("유효한 userId, productId로 생성하면, Like가 반환된다.")
        @Test
        void returnsLike_whenInputIsValid() {
            // Act
            Like like = new Like(1L, 2L);

            // Assert
            assertThat(like.getUserId()).isEqualTo(1L);
            assertThat(like.getProductId()).isEqualTo(2L);
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            // Act & Assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new Like(null, 2L));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // Act & Assert
            CoreException exception = assertThrows(CoreException.class,
                () -> new Like(1L, null));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
