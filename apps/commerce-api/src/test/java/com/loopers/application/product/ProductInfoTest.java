package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductInfoTest {

    @DisplayName("ProductInfo.from()")
    @Nested
    class From {

        @DisplayName("유효한 상품과 브랜드로 생성하면, ProductInfo를 반환한다.")
        @Test
        void returnsProductInfo_whenProductAndBrandAreValid() {
            // Arrange
            // Brand.getId() == 0L (BaseEntity 기본값), product.getBrandId()를 동일하게 맞춤
            Product product = new Product(0L, "상품A", 10000, 100, "설명", "https://img.url");
            Brand brand = new Brand("브랜드A", "브랜드 설명");

            // Act
            ProductInfo result = ProductInfo.from(product, brand);

            // Assert
            assertThat(result.name()).isEqualTo("상품A");
            assertThat(result.brandName()).isEqualTo("브랜드A");
        }

        @DisplayName("product가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIsNull() {
            // Arrange
            Brand brand = new Brand("브랜드A", "브랜드 설명");

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                ProductInfo.from(null, brand)
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("brand가 null이면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenBrandIsNull() {
            // Arrange
            Product product = new Product(1L, "상품A", 10000, 100, "설명", "https://img.url");

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                ProductInfo.from(product, null)
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("상품의 brandId와 브랜드의 id가 다르면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBrandIdMismatch() {
            // Arrange
            // product.getBrandId() == 99L, brand.getId() == 0L (BaseEntity 기본값) → 불일치
            Product product = new Product(99L, "상품A", 10000, 100, "설명", "https://img.url");
            Brand brand = new Brand("브랜드A", "브랜드 설명");

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                ProductInfo.from(product, brand)
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
