package com.loopers.infrastructure.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.config.redis.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class ProductCacheService {

    private static final String KEY_PREFIX = "product:detail:";
    private static final Duration TTL = Duration.ofMinutes(10);

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
}
