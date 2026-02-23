package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(Long brandId, String name, Integer price, Integer stock,
                                 String description, String imageUrl) {
        return productRepository.save(new Product(brandId, name, price, stock, description, imageUrl));
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(Long brandId, Pageable pageable) {
        return productRepository.findProducts(brandId, pageable);
    }

    @Transactional
    public Product updateProduct(Long id, String name, Integer price, Integer stock,
                                 String description, String imageUrl) {
        Product product = getProduct(id);
        product.update(name, price, stock, description, imageUrl);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        product.delete();
        productRepository.save(product);
    }
}
