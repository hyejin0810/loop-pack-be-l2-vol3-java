package com.loopers.interfaces.scheduler;

import com.loopers.infrastructure.ranking.RankingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@Component
public class RankingScheduler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RankingCacheService rankingCacheService;

    /**
     * 매일 자정에 전날 랭킹 점수의 10%를 오늘 키로 복사 (콜드 스타트 완화)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void carryOverRanking() {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
        String today = LocalDate.now().format(DATE_FORMATTER);

        log.info("[RankingScheduler] Score Carry-Over 시작: {} → {}", yesterday, today);
        rankingCacheService.carryOver(yesterday, today);
        log.info("[RankingScheduler] Score Carry-Over 완료");
    }
}
