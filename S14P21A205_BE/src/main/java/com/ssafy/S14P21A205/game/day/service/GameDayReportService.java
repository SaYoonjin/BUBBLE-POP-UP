package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final BigDecimal ZERO_REPUTATION = new BigDecimal("0.0");
    private static final BigDecimal MAX_REPUTATION = new BigDecimal("5.0");
    private static final BigDecimal REPUTATION_MULTIPLIER = new BigDecimal("5");
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(2, 4, 6);
    private static final SeasonTimeline SEASON_TIMELINE_POLICY = new SeasonTimeline();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final WeatherRepository weatherRepository;

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

        SeasonTimeline.DayTimeline timeline =
                SEASON_TIMELINE_POLICY.currentDay(state.startedAt(), day, store.getSeason().getTotalDays());
        if (LocalDateTime.now(clock).isBefore(timeline.businessEnd())) {
            return;
        }

        long revenue = valueOf(state.cumulativeSales());
        long totalCost = valueOf(state.cumulativeTotalCost());
        long netProfit = revenue - totalCost;
        DailyReport previousDayReport = day == 1
                ? null
                : dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1).orElse(null);
        int consecutiveDeficitDays = calculateConsecutiveDeficitDays(previousDayReport, netProfit);
        boolean isBankrupt = consecutiveDeficitDays >= 3;
        // TODO: 파산했을 때, 아이템 is_purchased 값을 false로 바꾸는 로직 추가 필요
        // and reset purchased items for the bankrupt user here.

        dailyReportRepository.save(DailyReport.create(
                store,
                day,
                store.getLocation().getLocationName(),
                store.getMenu().getMenuName(),
                safeToInt(revenue),
                safeToInt(totalCost),
                safeToInt(netProfit),
                defaultInt(state.cumulativeCustomerCount()),
                defaultInt(state.cumulativePurchaseCount()),
                defaultInt(state.stock()),
                consecutiveDeficitDays,
                isBankrupt,
                safeToInt(valueOf(state.balance())),
                normalizeCaptureRate(resolveCaptureRate(state))
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
                .map(this::normalizeCaptureRate)
                .orElse(ZERO_CAPTURE_RATE);

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
                toReputationScore(normalizeCaptureRate(report.getCaptureRate())),
                toReputationChange(normalizeCaptureRate(report.getCaptureRate()).subtract(previousCaptureRate)),
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

    private int calculateConsecutiveDeficitDays(DailyReport previousDayReport, long netProfit) {
        if (netProfit >= 0L) {
            return 0;
        }
        if (previousDayReport == null || previousDayReport.getConsecutiveDeficitDays() == null) {
            return 1;
        }
        return previousDayReport.getConsecutiveDeficitDays() + 1;
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

    private BigDecimal resolveCaptureRate(GameDayLiveState state) {
        if (state.inflowRate() != null) {
            return state.inflowRate();
        }
        if (state.startResponse() != null && state.startResponse().captureRate() != null) {
            return state.startResponse().captureRate();
        }
        return ZERO_CAPTURE_RATE;
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

    private BigDecimal toReputationScore(BigDecimal captureRate) {
        BigDecimal score = captureRate.multiply(REPUTATION_MULTIPLIER).setScale(1, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO_REPUTATION;
        }
        if (score.compareTo(MAX_REPUTATION) > 0) {
            return MAX_REPUTATION;
        }
        return score;
    }

    private BigDecimal toReputationChange(BigDecimal captureRateDelta) {
        return captureRateDelta.multiply(REPUTATION_MULTIPLIER).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCaptureRate(BigDecimal value) {
        if (value == null) {
            return ZERO_CAPTURE_RATE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
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
