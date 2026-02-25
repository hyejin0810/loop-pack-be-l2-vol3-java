package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(Long brandId, String name, Integer price, Integer stock,
                                 String description, String imageUrl) {
        return productRepository.save(new Product(brandId, name, price, stock, description, imageUrl));
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    public List<Product> getProductsByIds(List<Long> ids) {
        return productRepository.findAllByIds(ids);
    }

    public Page<Product> getProducts(Long brandId, Pageable pageable) {
        return productRepository.findProducts(brandId, pageable);
    }

    public Product updateProduct(Long id, String name, Integer price, Integer stock,
                                 String description, String imageUrl) {
        Product product = getProduct(id);
        product.update(name, price, stock, description, imageUrl);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        product.delete();
        productRepository.save(product);
    }

    public void deleteProductsByBrandId(Long brandId) {
        productRepository.findAllByBrandId(brandId).forEach(product -> {
            product.delete();
            productRepository.save(product);
        });
    }
}
