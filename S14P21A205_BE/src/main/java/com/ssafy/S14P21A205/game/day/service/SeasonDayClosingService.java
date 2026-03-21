package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.news.service.NewsService;
import com.ssafy.S14P21A205.game.season.service.SeasonFinalRankingService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeasonDayClosingService {

    private final SeasonRepository seasonRepository;
    private final StoreRepository storeRepository;
    private final GameDayReportService gameDayReportService;
    private final SeasonFinalRankingService seasonFinalRankingService;
    private final NewsService newsService;
    private final Executor dayClosingExecutor;

    public SeasonDayClosingService(
            SeasonRepository seasonRepository,
            StoreRepository storeRepository,
            GameDayReportService gameDayReportService,
            SeasonFinalRankingService seasonFinalRankingService,
            NewsService newsService,
            @Qualifier("dayClosingExecutor") Executor dayClosingExecutor
    ) {
        this.seasonRepository = seasonRepository;
        this.storeRepository = storeRepository;
        this.gameDayReportService = gameDayReportService;
        this.seasonFinalRankingService = seasonFinalRankingService;
        this.newsService = newsService;
        this.dayClosingExecutor = dayClosingExecutor;
    }

    public void handleBusinessEnd(Long seasonId, int day) {
        if (seasonId == null || day < 1) {
            return;
        }

        Season season = seasonRepository.findByIdAndStatus(seasonId, SeasonStatus.IN_PROGRESS).orElse(null);
        if (season == null || season.getTotalDays() == null || day > season.getTotalDays()) {
            return;
        }

        List<Store> stores = storeRepository.findBySeason_IdOrderByIdAsc(seasonId);
        if (stores.isEmpty()) {
            log.info("Skipping day closing. seasonId={} day={} reason=no_stores", seasonId, day);
            return;
        }

        boolean isLastDay = day == season.getTotalDays();

        // 일일 리포트 저장 (동기 — 스케줄러 catch-up 시 순서 보장 필요)
        for (Store store : stores) {
            gameDayReportService.recordClosedDayReport(store, day);
        }
        if (isLastDay) {
            seasonFinalRankingService.saveFinalRankings(season);
        }
        log.info("Daily reports saved. seasonId={} day={} storeCount={}", seasonId, day, stores.size());

        // 순위 집계 + 마감 뉴스는 비동기 (Redis 기반, daily_report 의존 없음)
        CompletableFuture.runAsync(() -> {
            try {
                newsService.updateDayRankingsFromRedis(seasonId, day, stores);
            } catch (Exception e) {
                log.error("Failed to update rankings/news from Redis. seasonId={} day={}", seasonId, day, e);
            }
        }, dayClosingExecutor);
    }
}
