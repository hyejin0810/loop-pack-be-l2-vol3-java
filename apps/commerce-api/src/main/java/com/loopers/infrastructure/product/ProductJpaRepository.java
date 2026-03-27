package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Product> findAllByBrandIdAndDeletedAtIsNull(Long brandId, Pageable pageable);
    List<Product> findAllByBrandIdAndDeletedAtIsNull(Long brandId);
    List<Product> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    @Modifying
    @Query("UPDATE Product p SET p.likesCount = p.likesCount + 1 WHERE p.id = :id")
    void incrementLikesCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.likesCount = p.likesCount - 1 WHERE p.id = :id AND p.likesCount > 0")
    void decrementLikesCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity AND p.deletedAt IS NULL")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id AND p.deletedAt IS NULL")
    void incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
