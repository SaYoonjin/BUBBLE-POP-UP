package com.ssafy.S14P21A205.game.day.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.news.service.NewsService;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.season.service.SeasonFinalRankingService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeasonDayClosingServiceTests {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private GameDayReportService gameDayReportService;

    @Mock
    private SeasonFinalRankingService seasonFinalRankingService;

    @Mock
    private NewsService newsService;

    @Mock
    private DayClosingJobService dayClosingJobService;

    private SeasonDayClosingService seasonDayClosingService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(dayClosingJobService.claim(any(Long.class), any(Integer.class)))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(gameDayReportService.recordClosedDayReport(any(Store.class), anyInt()))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(seasonFinalRankingService.saveFinalRankings(any(Season.class)))
                .thenReturn(true);
        seasonDayClosingService = new SeasonDayClosingService(
                seasonRepository,
                storeRepository,
                gameDayReportService,
                seasonFinalRankingService,
                newsService,
                dayClosingJobService,
                DIRECT_EXECUTOR
        );
    }

    @Test
    void handleBusinessEndSavesOnlyDailyReportsBeforeFinalDay() {
        Season season = season(9L, 7);
        Store firstStore = store(15L, season);
        Store secondStore = store(16L, season);

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(firstStore, secondStore));

        seasonDayClosingService.handleBusinessEnd(9L, 3);

        verify(gameDayReportService, times(2)).recordClosedDayReport(any(Store.class), eq(3));
        verify(seasonFinalRankingService, never()).saveFinalRankings(any(Season.class));
    }

    @Test
    void handleBusinessEndSavesDailyReportsAndFinalRankingsOnLastDay() {
        Season season = season(9L, 7);
        Store firstStore = store(15L, season);
        Store secondStore = store(16L, season);

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(firstStore, secondStore));

        seasonDayClosingService.handleBusinessEnd(9L, 7);

        verify(gameDayReportService, times(2)).recordClosedDayReport(any(Store.class), eq(7));
        verify(seasonFinalRankingService).saveFinalRankings(season);
    }

    @Test
    void handleBusinessEndTreatsDailyReportUniqueViolationAsIdempotentSuccess() {
        Season season = season(9L, 7);
        Store firstStore = store(15L, season);
        Store secondStore = store(16L, season);

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(firstStore, secondStore));
        when(gameDayReportService.recordClosedDayReport(any(Store.class), eq(7)))
                .thenThrow(new DataIntegrityViolationException("uk_daily_report_store_day"))
                .thenReturn(true);

        assertThatCode(() -> seasonDayClosingService.handleBusinessEnd(9L, 7))
                .doesNotThrowAnyException();

        verify(gameDayReportService, times(2)).recordClosedDayReport(any(Store.class), eq(7));
        verify(seasonFinalRankingService).saveFinalRankings(season);
    }

    @Test
    void handleBusinessEndTreatsFinalRankingUniqueViolationAsIdempotentSuccess() {
        Season season = season(9L, 7);
        Store store = store(15L, season);

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        doThrow(new DataIntegrityViolationException("uk_season_ranking_record_store"))
                .when(seasonFinalRankingService)
                .saveFinalRankings(season);

        assertThatCode(() -> seasonDayClosingService.handleBusinessEnd(9L, 7))
                .doesNotThrowAnyException();
    }

    @Test
    void handleBusinessEndRethrowsUnrelatedIntegrityViolation() {
        Season season = season(9L, 7);
        Store store = store(15L, season);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("another_constraint");

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        doThrow(failure).when(gameDayReportService).recordClosedDayReport(store, 7);

        assertThatThrownBy(() -> seasonDayClosingService.handleBusinessEnd(9L, 7))
                .isSameAs(failure);
        verify(dayClosingJobService).scheduleRetry(9L, 7, failure);
    }

    @Test
    void retryBusinessEndCompletesPendingJobAfterPreviousFailure() {
        Season season = season(9L, 7);
        Store store = store(15L, season);

        when(seasonRepository.findById(9L)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));

        seasonDayClosingService.retryBusinessEnd(9L, 7);

        verify(dayClosingJobService).claim(9L, 7);
        verify(dayClosingJobService).complete(9L, 7);
        verify(seasonFinalRankingService).saveFinalRankings(season);
    }

    @Test
    void handleBusinessEndSchedulesRetryWhenRedisStateCannotProduceReport() {
        Season season = season(9L, 7);
        Store store = store(15L, season);

        when(seasonRepository.findByIdAndStatus(9L, SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findAllBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(gameDayReportService.recordClosedDayReport(store, 7)).thenReturn(false);

        assertThatThrownBy(() -> seasonDayClosingService.handleBusinessEnd(9L, 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Daily report was not ready");

        verify(dayClosingJobService).scheduleRetry(eq(9L), eq(7), any(IllegalStateException.class));
        verify(dayClosingJobService, never()).complete(9L, 7);
        verify(seasonFinalRankingService, never()).saveFinalRankings(any(Season.class));
    }

    private Season season(Long seasonId, int totalDays) {
        Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", seasonId);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(season, "currentDay", totalDays);
        ReflectionTestUtils.setField(season, "totalDays", totalDays);
        ReflectionTestUtils.setField(season, "startTime", LocalDateTime.of(2026, 3, 18, 10, 0, 0));
        ReflectionTestUtils.setField(season, "endTime", LocalDateTime.of(2026, 3, 18, 10, 30, 0));
        return season;
    }

    private Store store(Long storeId, Season season) {
        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "season", season);
        return store;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
