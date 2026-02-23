package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @DisplayName("이름 마스킹")
    @Nested
    class GetMaskedName {

        @DisplayName("이름의 마지막 글자가 *으로 반환된다.")
        @Test
        void returnsNameWithLastCharMasked() {
            // Arrange
            User user = new User("testuser", "encrypted", "홍길동", "19900101", "test@example.com");

            // Act
            String maskedName = user.getMaskedName();

            // Assert
            assertThat(maskedName).isEqualTo("홍길*");
        }

        @DisplayName("한 글자 이름이면 *으로 반환된다.")
        @Test
        void returnsStar_whenNameIsSingleChar() {
            // Arrange
            User user = new User("testuser", "encrypted", "김", "19900101", "test@example.com");

            // Act
            String maskedName = user.getMaskedName();

            // Assert
            assertThat(maskedName).isEqualTo("*");
        }
    }

    @DisplayName("잔액 차감")
    @Nested
    class DeductBalance {

        @DisplayName("잔액이 충분하면, 차감 후 잔액이 감소한다.")
        @Test
        void deductsBalance_whenBalanceIsSufficient() {
            // Arrange
            User user = new User("testuser", "encrypted", "홍길동", "19900101", "test@example.com");
            user.restoreBalance(10000L);

            // Act
            user.deductBalance(3000L);

            // Assert
            assertThat(user.getBalance()).isEqualTo(7000L);
        }

        @DisplayName("잔액이 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBalanceIsInsufficient() {
            // Arrange
            User user = new User("testuser", "encrypted", "홍길동", "19900101", "test@example.com");
            user.restoreBalance(1000L);

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> user.deductBalance(3000L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("잔액 복구")
    @Nested
    class RestoreBalance {

        @DisplayName("잔액을 복구하면, 잔액이 증가한다.")
        @Test
        void restoresBalance() {
            // Arrange
            User user = new User("testuser", "encrypted", "홍길동", "19900101", "test@example.com");
            user.restoreBalance(5000L);

            // Act
            user.restoreBalance(3000L);

            // Assert
            assertThat(user.getBalance()).isEqualTo(8000L);
        }
    }
}
