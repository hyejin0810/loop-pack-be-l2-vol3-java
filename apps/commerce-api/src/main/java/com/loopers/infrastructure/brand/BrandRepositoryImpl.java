package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Brand> findByName(String name) {
        return brandJpaRepository.findByNameAndDeletedAtIsNull(name);
    }

    @Override
    public List<Brand> findAll() {
        return brandJpaRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public List<Brand> findAllByIds(Collection<Long> ids) {
        return brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }
}
