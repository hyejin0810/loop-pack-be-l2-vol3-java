package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandTest {

    @DisplayName("브랜드 생성")
    @Nested
    class Create {

        @DisplayName("이름이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new Brand(null, "스포츠 브랜드"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이름이 공백이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new Brand("  ", "스포츠 브랜드"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("유효한 이름으로 생성하면, 브랜드가 생성된다.")
        @Test
        void createsBrand_whenNameIsValid() {
            Brand brand = new Brand("나이키", "스포츠 브랜드");

            assertThat(brand.getName()).isEqualTo("나이키");
            assertThat(brand.getDescription()).isEqualTo("스포츠 브랜드");
        }
    }
}
