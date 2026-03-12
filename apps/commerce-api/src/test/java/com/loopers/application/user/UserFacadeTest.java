package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    @Mock
    private UserService userService;

    private UserFacade userFacade;

    @BeforeEach
    void setUp() {
        userFacade = new UserFacade(userService);
    }

    @DisplayName("내정보 조회")
    @Nested
    class GetMyInfo {

        @DisplayName("인증에 성공하면, 마스킹된 회원 정보를 반환한다.")
        @Test
        void returnsMaskedUserInfo_whenAuthenticated() {
            // Arrange
            String loginId = "testuser";
            String rawPassword = "Test1234!";
            User user = new User(loginId, "encrypted", "홍길동", "19900101", "test@example.com");
            given(userService.authenticate(loginId, rawPassword)).willReturn(user);

            // Act
            UserInfo result = userFacade.getMyInfo(loginId, rawPassword);

            // Assert
            assertThat(result.loginId()).isEqualTo("testuser");
            assertThat(result.name()).isEqualTo("홍길*");
        }

        @DisplayName("존재하지 않는 loginId이면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenLoginIdDoesNotExist() {
            // Arrange
            given(userService.authenticate("nouser", "Test1234!"))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다."));

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                userFacade.getMyInfo("nouser", "Test1234!")
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("비밀번호가 틀리면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenPasswordIsWrong() {
            // Arrange
            given(userService.authenticate("testuser", "wrongpw1!"))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "비밀번호가 일치하지 않습니다."));

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                userFacade.getMyInfo("testuser", "wrongpw1!")
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
