package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandService {

    private final BrandRepository brandRepository;

    public Brand register(String name, String description) {
        brandRepository.findByName(name).ifPresent(b -> {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드 이름입니다.");
        });
        return brandRepository.save(new Brand(name, description));
    }

    public Brand getBrand(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    public List<Brand> getBrands() {
        return brandRepository.findAll();
    }

    public List<Brand> getBrandsByIds(Collection<Long> ids) {
        return brandRepository.findAllByIds(ids);
    }

    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
        brand.delete();
        brandRepository.save(brand);
    }
}
