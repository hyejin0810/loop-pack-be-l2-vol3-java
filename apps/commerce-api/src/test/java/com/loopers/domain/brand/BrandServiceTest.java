package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository);
    }

    @DisplayName("브랜드 등록")
    @Nested
    class Register {

        @DisplayName("이미 존재하는 이름이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflict_whenNameAlreadyExists() {
            // Arrange
            given(brandRepository.findByName("나이키"))
                .willReturn(Optional.of(new Brand("나이키", "스포츠 브랜드")));

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> brandService.register("나이키", "또 다른 설명"));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("유효한 정보로 등록하면, 브랜드를 반환한다.")
        @Test
        void returnsBrand_whenInputIsValid() {
            // Arrange
            Brand brand = new Brand("나이키", "스포츠 브랜드");
            given(brandRepository.findByName("나이키")).willReturn(Optional.empty());
            given(brandRepository.save(any(Brand.class))).willReturn(brand);

            // Act
            Brand result = brandService.register("나이키", "스포츠 브랜드");

            // Assert
            assertThat(result.getName()).isEqualTo("나이키");
        }
    }

    @DisplayName("브랜드 단건 조회")
    @Nested
    class GetBrand {

        @DisplayName("존재하는 ID로 조회하면, 브랜드를 반환한다.")
        @Test
        void returnsBrand_whenIdExists() {
            // Arrange
            Brand brand = new Brand("나이키", "스포츠 브랜드");
            given(brandRepository.findById(1L)).willReturn(Optional.of(brand));

            // Act
            Brand result = brandService.getBrand(1L);

            // Assert
            assertThat(result.getName()).isEqualTo("나이키");
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenIdDoesNotExist() {
            // Arrange
            given(brandRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> brandService.getBrand(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("브랜드 목록 조회")
    @Nested
    class GetBrands {

        @DisplayName("전체 브랜드 목록을 반환한다.")
        @Test
        void returnsBrandList() {
            // Arrange
            given(brandRepository.findAll())
                .willReturn(List.of(new Brand("나이키", ""), new Brand("아디다스", "")));

            // Act
            List<Brand> result = brandService.getBrands();

            // Assert
            assertThat(result).hasSize(2);
        }
    }

    @DisplayName("브랜드 삭제")
    @Nested
    class DeleteBrand {

        @DisplayName("존재하는 ID로 삭제하면, 브랜드가 soft delete 된다.")
        @Test
        void softDeletesBrand_whenIdExists() {
            // Arrange
            Brand brand = new Brand("나이키", "스포츠 브랜드");
            given(brandRepository.findById(1L)).willReturn(Optional.of(brand));
            given(brandRepository.save(brand)).willReturn(brand);

            // Act
            brandService.deleteBrand(1L);

            // Assert
            assertThat(brand.getDeletedAt()).isNotNull();
        }

        @DisplayName("존재하지 않는 ID로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenIdDoesNotExist() {
            // Arrange
            given(brandRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            CoreException exception = assertThrows(CoreException.class,
                () -> brandService.deleteBrand(999L));

            // Assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
