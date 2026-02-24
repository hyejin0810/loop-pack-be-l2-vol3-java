package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @DisplayName("상품 등록")
    @Nested
    class CreateProduct {

        @DisplayName("유효한 정보로 등록하면, 상품을 반환한다.")
        @Test
        void returnsProduct_whenInputIsValid() {
            // Arrange
            Product product = new Product(1L, "나이키 신발", 10000, 100, "설명", "url");
            given(productRepository.save(any(Product.class))).willReturn(product);

            // Act
            Product result = productService.createProduct(1L, "나이키 신발", 10000, 100, "설명", "url");

            // Assert
            assertThat(result.getName()).isEqualTo("나이키 신발");
        }
    }

    @DisplayName("상품 단건 조회")
    @Nested
    class GetProduct {

        @DisplayName("존재하는 ID로 조회하면, 상품을 반환한다.")
        @Test
        void returnsProduct_whenIdExists() {
            // Arrange
            Product product = new Product(1L, "나이키 신발", 10000, 100, "설명", "url");
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            // Act
            Product result = productService.getProduct(1L);

            // Assert
            assertThat(result.getName()).isEqualTo("나이키 신발");
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenIdDoesNotExist() {
            // Arrange
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> productService.getProduct(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("상품 목록 조회")
    @Nested
    class GetProducts {

        @DisplayName("상품 목록을 페이지로 반환한다.")
        @Test
        void returnsProductPage() {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(
                new Product(1L, "나이키 신발", 10000, 100, "설명", "url")
            ));
            given(productRepository.findProducts(null, pageable)).willReturn(page);

            // Act
            Page<Product> result = productService.getProducts(null, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @DisplayName("상품 ID 목록 조회")
    @Nested
    class GetProductsByIds {

        @DisplayName("ID 목록으로 조회하면, 해당 상품 목록을 반환한다.")
        @Test
        void returnsProducts_whenIdsExist() {
            // Arrange
            List<Long> ids = List.of(1L, 2L);
            List<Product> products = List.of(
                new Product(1L, "나이키 신발", 10000, 100, "설명", "url1"),
                new Product(1L, "아디다스 티셔츠", 20000, 50, "설명", "url2")
            );
            given(productRepository.findAllByIds(ids)).willReturn(products);

            // Act
            List<Product> result = productService.getProductsByIds(ids);

            // Assert
            assertThat(result).hasSize(2);
        }

        @DisplayName("해당하는 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        void returnsEmpty_whenNoProductsFound() {
            // Arrange
            List<Long> ids = List.of(999L);
            given(productRepository.findAllByIds(ids)).willReturn(List.of());

            // Act
            List<Product> result = productService.getProductsByIds(ids);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("브랜드별 상품 일괄 삭제")
    @Nested
    class DeleteProductsByBrandId {

        @DisplayName("브랜드에 속한 상품이 있으면, 모두 soft delete 된다.")
        @Test
        void softDeletesAllProducts_whenBrandHasProducts() {
            // Arrange
            Product product1 = new Product(1L, "나이키 신발", 10000, 100, "설명", "url1");
            Product product2 = new Product(1L, "나이키 모자", 5000, 50, "설명", "url2");
            given(productRepository.findAllByBrandId(1L)).willReturn(List.of(product1, product2));
            given(productRepository.save(any(Product.class))).willAnswer(i -> i.getArgument(0));

            // Act
            productService.deleteProductsByBrandId(1L);

            // Assert
            assertThat(product1.getDeletedAt()).isNotNull();
            assertThat(product2.getDeletedAt()).isNotNull();
            verify(productRepository, times(2)).save(any(Product.class));
        }

        @DisplayName("브랜드에 속한 상품이 없으면, 아무것도 삭제하지 않는다.")
        @Test
        void doesNothing_whenBrandHasNoProducts() {
            // Arrange
            given(productRepository.findAllByBrandId(999L)).willReturn(List.of());

            // Act
            productService.deleteProductsByBrandId(999L);

            // Assert
            verify(productRepository, times(0)).save(any(Product.class));
        }
    }

    @DisplayName("상품 삭제")
    @Nested
    class DeleteProduct {

        @DisplayName("존재하는 ID로 삭제하면, 상품이 soft delete 된다.")
        @Test
        void softDeletesProduct_whenIdExists() {
            // Arrange
            Product product = new Product(1L, "나이키 신발", 10000, 100, "설명", "url");
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(productRepository.save(product)).willReturn(product);

            // Act
            productService.deleteProduct(1L);

            // Assert
            assertThat(product.getDeletedAt()).isNotNull();
        }

        @DisplayName("존재하지 않는 ID로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenIdDoesNotExist() {
            // Arrange
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> productService.deleteProduct(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
