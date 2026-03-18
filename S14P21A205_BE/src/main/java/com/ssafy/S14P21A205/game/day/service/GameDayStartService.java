package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.day.model.OpeningState;
import com.ssafy.S14P21A205.game.day.generator.PurchaseListGenerator;
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.policy.RentPolicy;
import com.ssafy.S14P21A205.game.day.policy.StoreRankingPolicy;
import com.ssafy.S14P21A205.game.day.resolver.EnvironmentScheduleResolver;
import com.ssafy.S14P21A205.game.day.resolver.EventScheduleResolver;
import com.ssafy.S14P21A205.game.day.resolver.NewsRankingResolver;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Festival;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.FestivalRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStartService {

    private static final int BUSINESS_OPEN_HOUR = GameTimePolicy.BUSINESS_OPEN_HOUR;
    private static final int BUSINESS_CLOSE_HOUR = GameTimePolicy.BUSINESS_CLOSE_HOUR;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final EnvironmentScheduleResolver environmentScheduleResolver;
    private final FestivalRepository festivalRepository;
    private final OrderRepository orderRepository;
    private final RentPolicy rentPolicy;
    private final CaptureRatePolicy captureRatePolicy;
    private final StoreRankingPolicy marketRankingPolicy;
    private final NewsRankingResolver newsRankingResolver;
    private final EventScheduleResolver eventScheduleResolver;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final PurchaseListGenerator purchaseListGenerator;
    private final Clock clock;

    @Transactional
    public GameDayStartResponse startDay(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        Store store = getActiveStore(user.getId());

        int day = resolveCurrentDay(store.getSeason());
        GameDayLiveState existingState = gameDayStoreStateRedisRepository.find(store.getId(), day).orElse(null);
        if (existingState != null && existingState.startResponse() != null) {
            return existingState.startResponse();
        }

        EnvironmentScheduleResolver.ResolvedEnvironment resolvedEnvironment = environmentScheduleResolver.resolve(
                store.getSeason().getId(),
                store.getLocation().getId(),
                day
        );
        DaySchedule daySchedule = resolvedEnvironment.daySchedule();
        List<Store> seasonStores = storeRepository.findBySeason_IdOrderByIdAsc(store.getSeason().getId());
        Festival festival = selectFestival(store.getLocation().getId(), store.getSeason().getId(), day);
        NewsRankingResolver.PreviousDayRanking previousDayRanking = newsRankingResolver.resolve(store, day);
        GameDayStartResponse.MarketSnapshot marketSnapshot =
                marketRankingPolicy.resolveSnapshot(
                        store,
                        seasonStores,
                        daySchedule,
                        festival,
                        previousDayRanking.areaEntryRank(),
                        previousDayRanking.trendKeywordRank()
                );
        List<Order> existingOrders = orderRepository.findDailyStartOrders(store.getId(), day);
        OpeningState openingState = rentPolicy.resolveStartingState(store, day, existingOrders, marketSnapshot);
        BigDecimal captureRate = captureRatePolicy.resolveStartingCaptureRate(marketSnapshot.priceBandMultiplier());
        List<GameDayStartResponse.EventSchedule> eventSchedule =
                eventScheduleResolver.resolve(
                        store.getSeason().getId(),
                        day,
                        store.getLocation().getId(),
                        store.getMenu().getId()
                );

        GameDayStartResponse response = new GameDayStartResponse(
                formatHour(BUSINESS_OPEN_HOUR),
                formatHour(BUSINESS_CLOSE_HOUR),
                daySchedule.hourlySchedule(),
                toWeatherLabel(resolvedEnvironment.weatherType()),
                resolvedEnvironment.weatherMultiplier(),
                daySchedule.dailyTrafficMultiplier(),
                captureRate,
                eventSchedule,
                openingState.initialBalance(),
                openingState.initialStock(),
                openingState.openingSummary(),
                marketSnapshot
        );

        writeInitialState(store, day, openingState, response);
        return response;
    }

    private Store getActiveStore(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private int resolveCurrentDay(Season season) {
        int currentDay = season.getCurrentDay() == null ? 1 : season.getCurrentDay();
        if (currentDay < 1 || currentDay > season.getTotalDays()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "Current season day is out of range.");
        }
        return currentDay;
    }

    private Festival selectFestival(Long locationId, Long seasonId, int day) {
        if (day != 4) {
            return null;
        }
        List<Festival> festivals = festivalRepository.findByLocationIdOrderByIdAsc(locationId);
        if (festivals.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(Math.toIntExact(seasonId - 1L), festivals.size());
        return festivals.get(index);
    }

    private void writeInitialState(
            Store store,
            int day,
            OpeningState openingState,
            GameDayStartResponse response
    ) {
        List<Integer> purchaseList = purchaseListGenerator.generate(response.hourlySchedule());
        LocalDateTime startedAt = LocalDateTime.now(clock);
        gameDayStoreStateRedisRepository.save(
                store.getId(),
                day,
                new GameDayLiveState(
                        startedAt,
                        purchaseList,
                        0,
                        response,
                        0,
                        0,
                        captureRatePolicy.normalizeCaptureRate(response.captureRate()),
                        store.getPrice(),
                        0,
                        0,
                        0L,
                        0,
                        0,
                        0L,
                        response.openingSummary() == null || response.openingSummary().fixedCostTotal() == null
                                ? 0L
                                : response.openingSummary().fixedCostTotal().longValue(),
                        (long) openingState.initialBalance(),
                        openingState.initialStock(),
                        startedAt
                )
        );
    }

    private String toWeatherLabel(WeatherType weatherType) {
        return weatherType.name();
    }

    private String formatHour(int hour) {
        return LocalTime.of(hour % 24, 0).format(TIME_FORMATTER);
    }
}
