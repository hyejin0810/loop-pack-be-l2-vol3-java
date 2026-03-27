package com.loopers.infrastructure.preorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.config.redis.RedisConfig;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * 가주문(PreOrder) Redis 캐시 서비스
 *
 * TTL 전략: Sliding TTL with Absolute Maximum
 * - 기본 TTL: 15분 (수정 시 리셋)
 * - 절대 최대: 30분 (PreOrder 생성 시각 기준, 이후 연장 불가)
 *
 * 내구성 전략 (durability):
 * - 현재 구현: Redis-only
 * - 대안 (권장): Write-through - Redis + DB(DRAFT 상태)를 동시에 저장
 *   → Redis 장애 시 DB에서 복구 가능
 *   → Payment 생성 시 pre-order 스냅샷을 Payment 레코드에 JSON으로 저장하여 보완
 */
@Slf4j
@Component
public class PreOrderCacheService {

    private static final String KEY_PREFIX = "pre-order:";
    private static final Duration SLIDING_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, String> defaultRedisTemplate;
    private final RedisTemplate<String, String> masterRedisTemplate;
    private final ObjectMapper objectMapper;

    public PreOrderCacheService(
        RedisTemplate<String, String> defaultRedisTemplate,
        @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> masterRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(PreOrder preOrder) {
        try {
            String json = objectMapper.writeValueAsString(preOrder);
            Duration ttl = calculateTtl(preOrder);
            masterRedisTemplate.opsForValue().set(key(preOrder.getPreOrderId()), json, ttl);
        } catch (Exception e) {
            log.error("[PreOrder] Redis 저장 실패: preOrderId={}", preOrder.getPreOrderId(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "가주문 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수정 시 TTL 리셋: min(SLIDING_TTL, 절대 만료까지 남은 시간)
     * 절대 만료를 넘겨서 TTL 연장하지 않음
     */
    public void saveAndResetTtl(PreOrder preOrder) {
        try {
            String json = objectMapper.writeValueAsString(preOrder);
            Duration ttl = calculateTtl(preOrder);
            masterRedisTemplate.opsForValue().set(key(preOrder.getPreOrderId()), json, ttl);
            log.debug("[PreOrder] TTL 리셋: preOrderId={}, ttl={}s", preOrder.getPreOrderId(), ttl.toSeconds());
        } catch (Exception e) {
            log.error("[PreOrder] Redis TTL 리셋 실패: preOrderId={}", preOrder.getPreOrderId(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "가주문 갱신 중 오류가 발생했습니다.");
        }
    }

    public Optional<PreOrder> get(String preOrderId) {
        try {
            String json = defaultRedisTemplate.opsForValue().get(key(preOrderId));
            if (json == null) {
                return Optional.empty();
            }
            PreOrder preOrder = objectMapper.readValue(json, PreOrder.class);
            // 절대 만료 시각 체크 (TTL이 남아있어도 절대 만료 초과 시 무효)
            if (preOrder.isExpired()) {
                delete(preOrderId);
                return Optional.empty();
            }
            return Optional.of(preOrder);
        } catch (Exception e) {
            log.warn("[PreOrder] Redis 조회 실패: preOrderId={}", preOrderId, e);
            return Optional.empty();
        }
    }

    public void delete(String preOrderId) {
        try {
            masterRedisTemplate.delete(key(preOrderId));
        } catch (Exception e) {
            log.warn("[PreOrder] Redis 삭제 실패: preOrderId={}", preOrderId, e);
        }
    }

    /**
     * Sliding TTL 계산: min(SLIDING_TTL, 절대 만료까지 남은 시간)
     * 절대 만료까지 남은 시간이 더 짧으면 그 값을 사용하여 절대 만료를 넘기지 않음
     */
    private Duration calculateTtl(PreOrder preOrder) {
        Duration untilAbsoluteDeadline = Duration.between(ZonedDateTime.now(), preOrder.getAbsoluteDeadline());
        if (untilAbsoluteDeadline.isNegative() || untilAbsoluteDeadline.isZero()) {
            return Duration.ofSeconds(1); // 이미 만료, 최소값으로 설정
        }
        return untilAbsoluteDeadline.compareTo(SLIDING_TTL) < 0
            ? untilAbsoluteDeadline
            : SLIDING_TTL;
    }

    private String key(String preOrderId) {
        return KEY_PREFIX + preOrderId;
    }
}
