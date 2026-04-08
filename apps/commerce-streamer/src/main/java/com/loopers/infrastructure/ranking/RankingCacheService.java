package com.loopers.infrastructure.ranking;

import com.loopers.config.redis.RedisConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RankingCacheService {

    static final String KEY_PREFIX = "ranking:all:";
    static final Duration TTL = Duration.ofDays(2);

    private final RedisTemplate<String, String> masterRedisTemplate;

    public RankingCacheService(
        @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.masterRedisTemplate = masterRedisTemplate;
    }

    public void incrementScore(String date, Long productId, double score) {
        String key = KEY_PREFIX + date;
        masterRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
        masterRedisTemplate.expire(key, TTL);
    }
}
