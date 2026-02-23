package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @DisplayName("상품 생성")
    @Nested
    class Create {

        @DisplayName("이름이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new Product(1L, null, 10000, 100, "설명", "url"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenPriceIsZeroOrNegative() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new Product(1L, "나이키 신발", 0, 100, "설명", "url"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsNegative() {
            CoreException exception = assertThrows(CoreException.class,
                () -> new Product(1L, "나이키 신발", 10000, -1, "설명", "url"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("유효한 정보로 생성하면, 상품이 생성된다.")
        @Test
        void createsProduct_whenInputIsValid() {
            Product product = new Product(1L, "나이키 신발", 10000, 100, "설명", "url");

            assertThat(product.getName()).isEqualTo("나이키 신발");
            assertThat(product.getPrice()).isEqualTo(10000);
            assertThat(product.getStock()).isEqualTo(100);
            assertThat(product.getLikesCount()).isEqualTo(0);
        }
    }

    @DisplayName("재고 차감")
    @Nested
    class DecreaseStock {

        @DisplayName("재고가 충분하면, 차감 후 재고가 감소한다.")
        @Test
        void decreasesStock_whenStockIsSufficient() {
            Product product = new Product(1L, "나이키 신발", 10000, 10, "설명", "url");

            product.decreaseStock(3);

            assertThat(product.getStock()).isEqualTo(7);
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsInsufficient() {
            Product product = new Product(1L, "나이키 신발", 10000, 2, "설명", "url");

            CoreException exception = assertThrows(CoreException.class,
                () -> product.decreaseStock(5));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("재고 복구")
    @Nested
    class IncreaseStock {

        @DisplayName("재고를 복구하면, 재고가 증가한다.")
        @Test
        void increasesStock() {
            Product product = new Product(1L, "나이키 신발", 10000, 5, "설명", "url");

            product.increaseStock(3);

            assertThat(product.getStock()).isEqualTo(8);
        }
    }

    @DisplayName("좋아요 수 증감")
    @Nested
    class LikesCount {

        @DisplayName("좋아요를 등록하면, likesCount가 1 증가한다.")
        @Test
        void increasesLikesCount() {
            Product product = new Product(1L, "나이키 신발", 10000, 10, "설명", "url");

            product.increaseLikes();

            assertThat(product.getLikesCount()).isEqualTo(1);
        }

        @DisplayName("좋아요를 취소하면, likesCount가 1 감소한다.")
        @Test
        void decreasesLikesCount() {
            Product product = new Product(1L, "나이키 신발", 10000, 10, "설명", "url");
            product.increaseLikes();

            product.decreaseLikes();

            assertThat(product.getLikesCount()).isEqualTo(0);
        }
    }
}
