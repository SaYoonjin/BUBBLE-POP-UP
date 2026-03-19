package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.generator.PurchaseListGenerator;
import com.ssafy.S14P21A205.game.day.policy.BankruptcyPolicy;
import com.ssafy.S14P21A205.game.day.policy.ProfitPolicy;
import com.ssafy.S14P21A205.game.day.policy.ReputationPolicy;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.WeatherLocation;
import com.ssafy.S14P21A205.game.environment.repository.WeatherDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherLocationRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.model.SeasonPhase;
import com.ssafy.S14P21A205.game.time.model.SeasonTimePoint;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.store.service.StoreLocationTransitionSupport;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayReportService {

    private static final int MAX_SUPPORTED_DAY = 7;
    private static final int STOCK_DISPOSED_COUNT = 0;
    private static final BigDecimal ZERO_CAPTURE_RATE = new BigDecimal("0.00");
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(1, 3, 5, 7);
    private static final SeasonTimelineService SEASON_TIMELINE_SERVICE = new SeasonTimelineService();
    private static final StoreLocationTransitionSupport STORE_LOCATION_TRANSITION_SUPPORT = new StoreLocationTransitionSupport();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final WeatherDayRedisRepository weatherDayRedisRepository;
    private final WeatherLocationRepository weatherLocationRepository;
    private final ProfitPolicy profitPolicy;
    private final ReputationPolicy reputationPolicy;
    private final BankruptcyPolicy bankruptcyPolicy;
    private final GameDayStateService gameDayStateService;
    private final PurchaseListGenerator purchaseListGenerator;
    private final Clock clock;

    @Transactional
    public void recordClosedDayReport(Store store) {
        LocalDateTime now = LocalDateTime.now(clock);
        SeasonTimePoint seasonTimePoint = SEASON_TIMELINE_SERVICE.resolve(store.getSeason(), now);
        Integer day = resolveReportDay(store.getSeason(), seasonTimePoint);
        if (day == null) {
            return;
        }
        recordClosedDayReport(store, day, now, seasonTimePoint);
    }

    @Transactional
    public void recordClosedDayReport(Store store, int day) {
        if (store == null || day < 1) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        SeasonTimePoint seasonTimePoint = SEASON_TIMELINE_SERVICE.resolve(store.getSeason(), now);
        recordClosedDayReport(store, day, now, seasonTimePoint);
    }

    private void recordClosedDayReport(
            Store store,
            int day,
            LocalDateTime now,
            SeasonTimePoint seasonTimePoint
    ) {
        Integer totalDays = store.getSeason().getTotalDays();
        if (totalDays != null && day > totalDays) {
            return;
        }
        if (dailyReportRepository.existsByStoreIdAndDay(store.getId(), day)) {
            return;
        }

        if (shouldRefreshCurrentDayState(day, seasonTimePoint)) {
            gameDayStateService.refreshGameState(store);
        }

        GameDayLiveState state = gameDayStoreStateRedisRepository.find(store.getId(), day).orElse(null);
        if (state == null || state.startedAt() == null) {
            return;
        }

        DayWindow timeline = SEASON_TIMELINE_SERVICE.day(store.getSeason(), day);
        if (now.isBefore(timeline.businessEnd())) {
            return;
        }

        ProfitPolicy.ProfitResult profitResult =
                profitPolicy.calculate(state.cumulativeSales(), state.cumulativeTotalCost());
        DailyReport previousDayReport = day == 1
                ? null
                : dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1).orElse(null);
        BankruptcyPolicy.BankruptcyResult bankruptcyResult =
                bankruptcyPolicy.resolve(previousDayReport, profitResult.netProfit());

        dailyReportRepository.save(DailyReport.create(
                store,
                day,
                store.getLocation().getLocationName(),
                store.getMenu().getMenuName(),
                safeToInt(profitResult.revenue()),
                safeToInt(profitResult.totalCost()),
                safeToInt(profitResult.netProfit()),
                defaultInt(state.cumulativeCustomerCount()),
                defaultInt(state.cumulativePurchaseCount()),
                defaultInt(state.stock()),
                bankruptcyResult.consecutiveDeficitDays(),
                bankruptcyResult.bankrupt(),
                safeToInt(valueOf(state.balance())),
                reputationPolicy.normalizeCaptureRate(reputationPolicy.resolveCaptureRate(state))
        ));
        store.changePurchaseCursor(
                purchaseListGenerator.advanceCursor(store.getPurchaseCursor(), defaultInt(state.purchaseCursor()))
        );
    }

    public GameDayReportResponse getDayReport(Authentication authentication, int day) {
        User user = userService.getCurrentUser(authentication);
        Store store = getReportStore(user.getId());
        validateDay(day, store.getSeason());

        DailyReport report = dailyReportRepository.findByStoreIdAndDay(store.getId(), day)
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND));

        BigDecimal previousCaptureRate = day == 1
                ? ZERO_CAPTURE_RATE
                : dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .map(DailyReport::getCaptureRate)
                .map(reputationPolicy::normalizeCaptureRate)
                .orElse(ZERO_CAPTURE_RATE);
        BigDecimal captureRate = reputationPolicy.normalizeCaptureRate(report.getCaptureRate());

        return new GameDayReportResponse(
                report.getStore().getSeason().getId(),
                report.getDay(),
                resolveLocationName(report),
                resolveMenuName(report),
                valueOf(report.getRevenue()),
                valueOf(report.getTotalCost()),
                valueOf(report.getNetProfit()),
                defaultInt(report.getVisitors()),
                defaultInt(report.getSalesCount()),
                defaultInt(report.getStockRemaining()),
                STOCK_DISPOSED_COUNT,
                reputationPolicy.toReputationScore(captureRate),
                reputationPolicy.toReputationChange(captureRate.subtract(previousCaptureRate)),
                resolveTomorrowWeather(
                        store.getSeason().getId(),
                        resolveTomorrowLocationId(store, report.getDay()),
                        report.getDay(),
                        store.getSeason().getTotalDays()
                ),
                resolveIsNextDayOrderDay(report.getDay(), store.getSeason().getTotalDays()),
                defaultInt(report.getConsecutiveDeficitDays()),
                Boolean.TRUE.equals(report.getIsBankrupt())
        );
    }

    private Store getReportStore(Integer userId) {
        Optional<Store> activeStore = storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(
                userId,
                SeasonStatus.IN_PROGRESS
        );
        if (activeStore.isPresent()) {
            STORE_LOCATION_TRANSITION_SUPPORT.applyPendingLocationIfDue(activeStore.get(), LocalDateTime.now(clock));
            return activeStore.get();
        }

        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.FINISHED)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_PARTICIPATING));
    }

    private void validateDay(int day, Season season) {
        int totalDays = season.getTotalDays() == null ? MAX_SUPPORTED_DAY : season.getTotalDays();
        if (day < 1 || day > MAX_SUPPORTED_DAY || day > totalDays) {
            throw new BaseException(
                    ErrorCode.INVALID_DAY,
                    "day must be between 1 and %d.".formatted(Math.min(MAX_SUPPORTED_DAY, totalDays))
            );
        }
    }

    private Integer resolveReportDay(Season season, SeasonTimePoint seasonTimePoint) {
        Integer currentDay = seasonTimePoint.currentDay();
        return switch (seasonTimePoint.phase()) {
            case LOCATION_SELECTION -> null;
            case DAY_PREPARING, DAY_BUSINESS -> currentDay == null || currentDay <= 1 ? null : currentDay - 1;
            case DAY_REPORT -> currentDay;
            case SEASON_SUMMARY, NEXT_SEASON_WAITING, CLOSED -> season.getTotalDays();
        };
    }

    private boolean shouldRefreshCurrentDayState(Integer reportDay, SeasonTimePoint seasonTimePoint) {
        return reportDay != null
                && seasonTimePoint.phase() == SeasonPhase.DAY_REPORT
                && seasonTimePoint.currentDay() != null
                && reportDay.equals(seasonTimePoint.currentDay());
    }

    private GameDayReportResponse.TomorrowWeather resolveTomorrowWeather(
            Long seasonId,
            Long locationId,
            int day,
            int totalDays
    ) {
        if (day >= totalDays) {
            return null;
        }

        return weatherDayRedisRepository.findLocation(seasonId, locationId, day + 1)
                .or(() -> loadAndCacheTomorrow(seasonId, locationId, day + 1))
                .map(entry -> new GameDayReportResponse.TomorrowWeather(entry.weatherType().name()))
                .orElse(null);
    }

    private Optional<WeatherDayRedisRepository.WeatherDayEntry> loadAndCacheTomorrow(Long seasonId, Long locationId, int day) {
        java.util.List<WeatherLocation> dayEntries = weatherLocationRepository.findByDayOrderByLocation_IdAsc(day);
        if (dayEntries.isEmpty()) {
            return Optional.empty();
        }

        java.util.List<WeatherDayRedisRepository.WeatherDayEntry> cachedEntries = dayEntries.stream()
                .map(entry -> new WeatherDayRedisRepository.WeatherDayEntry(
                        entry.getLocation().getId(),
                        entry.getDay(),
                        entry.getWeather().getWeatherType(),
                        entry.getWeather().getPopulationPercent()
                ))
                .toList();
        weatherDayRedisRepository.saveDay(seasonId, day, cachedEntries);
        return cachedEntries.stream()
                .filter(entry -> locationId.equals(entry.locationId()))
                .findFirst();
    }

    private Boolean resolveIsNextDayOrderDay(int day, int totalDays) {
        if (day >= totalDays) {
            return null;
        }
        return REGULAR_ORDER_DAYS.contains(day + 1);
    }

    private Long resolveTomorrowLocationId(Store store, int day) {
        if (store == null || store.getLocation() == null) {
            return null;
        }
        if (day >= (store.getSeason() == null || store.getSeason().getTotalDays() == null ? MAX_SUPPORTED_DAY : store.getSeason().getTotalDays())) {
            return store.getLocation().getId();
        }
        return STORE_LOCATION_TRANSITION_SUPPORT.resolveLocationForDay(store, day + 1).getId();
    }

    private String resolveLocationName(DailyReport report) {
        if (report.getLocationName() != null && !report.getLocationName().isBlank()) {
            return report.getLocationName();
        }
        return report.getStore().getLocation().getLocationName();
    }

    private String resolveMenuName(DailyReport report) {
        if (report.getMenuName() != null && !report.getMenuName().isBlank()) {
            return report.getMenuName();
        }
        return report.getStore().getMenu().getMenuName();
    }

    private int safeToInt(long value) {
        return Math.toIntExact(value);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private long valueOf(Long value) {
        return value == null ? 0L : value;
    }
}
