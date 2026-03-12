package com.loopers.infrastructure.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.config.redis.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class ProductCacheService {

    private static final String KEY_PREFIX = "product:detail:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private static final String LIST_KEY_PREFIX = "product:list:";
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    // 목록 캐시 직렬화용 내부 레코드
    record ProductListCache(List<ProductInfo> content, long totalElements, int pageNumber, int pageSize) {}

    private final RedisTemplate<String, String> defaultRedisTemplate;  // 읽기 (Replica)
    private final RedisTemplate<String, String> masterRedisTemplate;   // 쓰기 (Master)
    private final ObjectMapper objectMapper;

    public ProductCacheService(
            RedisTemplate<String, String> defaultRedisTemplate,
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> masterRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // 캐시 조회 - Redis 장애 시 Optional.empty() 반환하여 DB 폴백
    public Optional<ProductInfo> get(Long productId) {
        try {
            String json = defaultRedisTemplate.opsForValue().get(KEY_PREFIX + productId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ProductInfo.class));
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - productId: {}, error: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    // 캐시 저장 - TTL 10분
    public void set(ProductInfo productInfo) {
        try {
            String json = objectMapper.writeValueAsString(productInfo);
            masterRedisTemplate.opsForValue().set(KEY_PREFIX + productInfo.id(), json, TTL);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - productId: {}, error: {}", productInfo.id(), e.getMessage());
        }
    }

    // 캐시 무효화
    public void delete(Long productId) {
        try {
            masterRedisTemplate.delete(KEY_PREFIX + productId);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - productId: {}, error: {}", productId, e.getMessage());
        }
    }

    // 목록 캐시 조회
    public Optional<Page<ProductInfo>> getList(Long brandId, String sort, int pageNumber, int pageSize) {
        try {
            String json = defaultRedisTemplate.opsForValue().get(listKey(brandId, sort, pageNumber, pageSize));
            if (json == null) {
                return Optional.empty();
            }
            ProductListCache cache = objectMapper.readValue(json, ProductListCache.class);
            Page<ProductInfo> page = new PageImpl<>(
                cache.content(),
                PageRequest.of(cache.pageNumber(), cache.pageSize()),
                cache.totalElements()
            );
            return Optional.of(page);
        } catch (Exception e) {
            log.warn("목록 캐시 조회 실패 - brandId: {}, sort: {}, error: {}", brandId, sort, e.getMessage());
            return Optional.empty();
        }
    }

    // 목록 캐시 저장
    public void setList(Long brandId, String sort, Page<ProductInfo> page) {
        try {
            ProductListCache cache = new ProductListCache(
                page.getContent(), page.getTotalElements(),
                page.getNumber(), page.getSize()
            );
            String json = objectMapper.writeValueAsString(cache);
            masterRedisTemplate.opsForValue().set(
                listKey(brandId, sort, page.getNumber(), page.getSize()), json, LIST_TTL
            );
        } catch (Exception e) {
            log.warn("목록 캐시 저장 실패 - brandId: {}, sort: {}, error: {}", brandId, sort, e.getMessage());
        }
    }

    // 목록 캐시 전체 무효화 (product:list:* 패턴)
    public void deleteListAll() {
        try {
            Set<String> keys = masterRedisTemplate.keys(LIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                masterRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("목록 캐시 전체 삭제 실패: {}", e.getMessage());
        }
    }

    private String listKey(Long brandId, String sort, int pageNumber, int pageSize) {
        String brand = brandId != null ? String.valueOf(brandId) : "all";
        return LIST_KEY_PREFIX + brand + ":" + sort + ":" + pageNumber + ":" + pageSize;
    }
}
