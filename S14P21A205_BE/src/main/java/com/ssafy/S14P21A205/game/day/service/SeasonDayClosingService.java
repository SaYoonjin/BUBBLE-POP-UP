package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.game.news.service.NewsService;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.season.service.SeasonFinalRankingService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeasonDayClosingService {

    private static final String DAILY_REPORT_UNIQUE_CONSTRAINT = "uk_daily_report_store_day";
    private static final String FINAL_RANKING_UNIQUE_CONSTRAINT = "uk_season_ranking_record_store";
    private static final String DAY_CLOSING_JOB_UNIQUE_CONSTRAINT = "uk_day_closing_job_season_day";

    private final SeasonRepository seasonRepository;
    private final StoreRepository storeRepository;
    private final GameDayReportService gameDayReportService;
    private final SeasonFinalRankingService seasonFinalRankingService;
    private final NewsService newsService;
    private final DayClosingJobService dayClosingJobService;
    private final Executor dayClosingExecutor;

    public SeasonDayClosingService(
            SeasonRepository seasonRepository,
            StoreRepository storeRepository,
            GameDayReportService gameDayReportService,
            SeasonFinalRankingService seasonFinalRankingService,
            NewsService newsService,
            DayClosingJobService dayClosingJobService,
            @Qualifier("dayClosingExecutor") Executor dayClosingExecutor
    ) {
        this.seasonRepository = seasonRepository;
        this.storeRepository = storeRepository;
        this.gameDayReportService = gameDayReportService;
        this.seasonFinalRankingService = seasonFinalRankingService;
        this.newsService = newsService;
        this.dayClosingJobService = dayClosingJobService;
        this.dayClosingExecutor = dayClosingExecutor;
    }

    public void handleBusinessEnd(Long seasonId, int day) {
        if (seasonId == null || day < 1) {
            return;
        }

        Season season = seasonRepository.findByIdAndStatus(seasonId, SeasonStatus.IN_PROGRESS).orElse(null);
        if (season == null || season.resolveRuntimePlayableDays() <= 0 || day > season.resolveRuntimePlayableDays()) {
            return;
        }

        try {
            dayClosingJobService.registerIfMissing(season, day);
        } catch (DataIntegrityViolationException e) {
            if (!isConstraintViolation(e, DAY_CLOSING_JOB_UNIQUE_CONSTRAINT)) {
                throw e;
            }
        }
        executeRegisteredClosing(season, day);
    }

    public void retryBusinessEnd(Long seasonId, int day) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalStateException("Season not found for day closing retry."));
        int totalDays = season.resolveRuntimePlayableDays();
        if (totalDays <= 0 || day < 1 || day > totalDays) {
            throw new IllegalStateException("Invalid day closing retry target.");
        }
        executeRegisteredClosing(season, day);
    }

    private void executeRegisteredClosing(Season season, int day) {
        Long seasonId = season.getId();
        if (!dayClosingJobService.claim(seasonId, day)) {
            return;
        }

        List<Store> stores;
        try {
            stores = closeCoreData(season, day);
            dayClosingJobService.complete(seasonId, day);
        } catch (RuntimeException e) {
            try {
                dayClosingJobService.scheduleRetry(seasonId, day, e);
            } catch (RuntimeException retryFailure) {
                e.addSuppressed(retryFailure);
            }
            throw e;
        }

        scheduleNews(seasonId, day, stores);
    }

    private List<Store> closeCoreData(Season season, int day) {
        Long seasonId = season.getId();
        List<Store> stores = storeRepository.findAllBySeason_IdOrderByIdAsc(seasonId);
        if (stores.isEmpty()) {
            log.info("Skipping day closing. seasonId={} day={} reason=no_stores", seasonId, day);
            return stores;
        }

        for (Store store : stores) {
            try {
                boolean completed = gameDayReportService.recordClosedDayReport(store, day);
                if (!completed) {
                    throw new IllegalStateException(
                            "Daily report was not ready. storeId=%d day=%d".formatted(store.getId(), day)
                    );
                }
            } catch (DataIntegrityViolationException e) {
                if (!isConstraintViolation(e, DAILY_REPORT_UNIQUE_CONSTRAINT)) {
                    throw e;
                }
                log.info(
                        "Daily report already committed by another closing task. seasonId={} storeId={} day={}",
                        seasonId,
                        store.getId(),
                        day
                );
            }
        }

        if (day == season.resolveRuntimePlayableDays()) {
            try {
                if (!seasonFinalRankingService.saveFinalRankings(season)) {
                    throw new IllegalStateException("Final rankings were not ready. seasonId=" + seasonId);
                }
            } catch (DataIntegrityViolationException e) {
                if (!isConstraintViolation(e, FINAL_RANKING_UNIQUE_CONSTRAINT)) {
                    throw e;
                }
                log.info("Final rankings already committed by another closing task. seasonId={}", seasonId);
            }
        }

        log.info("Daily reports saved. seasonId={} day={} storeCount={}", seasonId, day, stores.size());
        return stores;
    }

    private void scheduleNews(Long seasonId, int day, List<Store> stores) {
        if (stores.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    newsService.updateDayRankingsFromRedis(seasonId, day, stores);
                } catch (Exception e) {
                    log.error("Failed to update rankings/news from Redis. seasonId={} day={}", seasonId, day, e);
                }
            }, dayClosingExecutor);
        } catch (RuntimeException e) {
            log.error("Failed to submit rankings/news task. seasonId={} day={}", seasonId, day, e);
        }
    }

    private boolean isConstraintViolation(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && containsIgnoreCase(constraintViolationException.getConstraintName(), constraintName)) {
                return true;
            }
            if (containsIgnoreCase(current.getMessage(), constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null
                && expected != null
                && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
