package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
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
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @Mock
    private ProductService productService;

    @Mock
    private BrandService brandService;

    private ProductFacade productFacade;

    @BeforeEach
    void setUp() {
        productFacade = new ProductFacade(productService, brandService);
    }

    @DisplayName("상품 목록 조회")
    @Nested
    class GetProducts {

        @DisplayName("모든 브랜드 정보가 존재하면, 상품 목록을 반환한다.")
        @Test
        void returnsProductPage_whenAllBrandsExist() {
            // Arrange
            Long brandId = 1L;
            // Brand.getId() == 0L (BaseEntity 기본값), product.getBrandId()를 동일하게 맞춤
            Product product = new Product(0L, "상품A", 10000, 100, "설명", "https://img.url");
            Brand brand = new Brand("브랜드A", "브랜드 설명");
            Page<Product> productPage = new PageImpl<>(List.of(product));

            given(productService.getProducts(eq(brandId), any(Pageable.class))).willReturn(productPage);
            given(brandService.getBrandsByIds(any())).willReturn(List.of(brand));

            // Act
            Page<ProductInfo> result = productFacade.getProducts(brandId, "latest", PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("상품A");
            assertThat(result.getContent().get(0).brandName()).isEqualTo("브랜드A");
        }

        @DisplayName("일부 상품의 브랜드 정보가 누락되면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenBrandIsMissingForProduct() {
            // Arrange
            Long brandId = 1L;
            // brandId=99L 상품인데, brandService는 해당 브랜드를 반환하지 않음
            Product product = new Product(99L, "고아상품", 5000, 10, "설명", "https://img.url");
            Page<Product> productPage = new PageImpl<>(List.of(product));

            given(productService.getProducts(eq(brandId), any(Pageable.class))).willReturn(productPage);
            given(brandService.getBrandsByIds(any())).willReturn(List.of());

            // Act
            CoreException exception = assertThrows(CoreException.class, () ->
                productFacade.getProducts(brandId, "latest", PageRequest.of(0, 10))
            );

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
