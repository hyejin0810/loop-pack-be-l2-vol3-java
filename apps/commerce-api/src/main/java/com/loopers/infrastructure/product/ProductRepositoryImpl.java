package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Product> findProducts(Long brandId, Pageable pageable) {
        if (brandId != null) {
            return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable);
        }
        return productJpaRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public List<Product> findAllByBrandId(Long brandId) {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId);
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public void incrementLikesCount(Long id) {
        productJpaRepository.incrementLikesCount(id);
    }

    @Override
    public void decrementLikesCount(Long id) {
        productJpaRepository.decrementLikesCount(id);
    }

    @Override
    public int decrementStock(Long id, int quantity) {
        return productJpaRepository.decrementStock(id, quantity);
    }

    @Override
    public void incrementStock(Long id, int quantity) {
        productJpaRepository.incrementStock(id, quantity);
    }
}
