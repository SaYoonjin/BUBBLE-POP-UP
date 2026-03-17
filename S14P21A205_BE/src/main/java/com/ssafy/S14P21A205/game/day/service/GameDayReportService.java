package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.policy.BankruptcyPolicy;
import com.ssafy.S14P21A205.game.day.policy.ProfitPolicy;
import com.ssafy.S14P21A205.game.day.policy.ReputationPolicy;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(2, 4, 6);
    private static final SeasonTimelineService SEASON_TIMELINE_SERVICE = new SeasonTimelineService();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final WeatherRepository weatherRepository;
    private final ProfitPolicy profitPolicy;
    private final ReputationPolicy reputationPolicy;
    private final BankruptcyPolicy bankruptcyPolicy;

    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public void recordClosedDayReport(Store store) {
        int day = resolveCurrentDay(store.getSeason());
        if (dailyReportRepository.existsByStoreIdAndDay(store.getId(), day)) {
            return;
        }

        GameDayLiveState state = gameDayStoreStateRedisRepository.find(store.getId(), day).orElse(null);
        if (state == null || state.startedAt() == null) {
            return;
        }

        DayWindow timeline =
                SEASON_TIMELINE_SERVICE.currentDay(state.startedAt(), day, store.getSeason().getTotalDays());
        if (LocalDateTime.now(clock).isBefore(timeline.businessEnd())) {
            return;
        }

        ProfitPolicy.ProfitResult profitResult =
                profitPolicy.calculate(state.cumulativeSales(), state.cumulativeTotalCost());
        DailyReport previousDayReport = day == 1
                ? null
                : dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1).orElse(null);
        BankruptcyPolicy.BankruptcyResult bankruptcyResult =
                bankruptcyPolicy.resolve(previousDayReport, profitResult.netProfit());
        // TODO: 파산했을 때, 아이템 is_purchased 값을 false로 바꾸는 로직 추가 필요
        // and reset purchased items for the bankrupt user here.

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
                resolveTomorrowWeather(report.getDay(), store.getSeason().getTotalDays()),
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

    private int resolveCurrentDay(Season season) {
        int currentDay = season.getCurrentDay() == null ? 1 : season.getCurrentDay();
        if (currentDay < 1 || currentDay > season.getTotalDays()) {
            throw new BaseException(ErrorCode.INVALID_DAY, "Current season day is out of range.");
        }
        return currentDay;
    }

    private GameDayReportResponse.TomorrowWeather resolveTomorrowWeather(int day, int totalDays) {
        if (day >= totalDays) {
            return null;
        }

        List<Weather> weathers = weatherRepository.findAllByOrderByIdAsc();
        if (weathers.isEmpty()) {
            return null;
        }

        Weather tomorrowWeather = weathers.get(Math.floorMod(day, weathers.size()));
        return new GameDayReportResponse.TomorrowWeather(tomorrowWeather.getWeatherType().name());
    }

    private Boolean resolveIsNextDayOrderDay(int day, int totalDays) {
        if (day >= totalDays) {
            return null;
        }
        return REGULAR_ORDER_DAYS.contains(day + 1);
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
