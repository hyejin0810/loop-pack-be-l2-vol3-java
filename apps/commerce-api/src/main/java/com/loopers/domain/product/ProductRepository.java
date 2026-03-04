package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    Page<Product> findProducts(Long brandId, Pageable pageable);
    List<Product> findAllByBrandId(Long brandId);
    List<Product> findAllByIds(List<Long> ids);
    Product save(Product product);
    void incrementLikesCount(Long id);
    void decrementLikesCount(Long id);
    int decrementStock(Long id, int quantity);
    void incrementStock(Long id, int quantity);
}
