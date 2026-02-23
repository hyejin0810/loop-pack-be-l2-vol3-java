package com.loopers.domain.brand;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    Optional<Brand> findById(Long id);
    Optional<Brand> findByName(String name);
    List<Brand> findAll();
    List<Brand> findAllByIds(Collection<Long> ids);
    Brand save(Brand brand);
}
