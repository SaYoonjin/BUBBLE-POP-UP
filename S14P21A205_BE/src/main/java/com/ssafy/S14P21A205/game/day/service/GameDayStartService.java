package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.day.model.OpeningState;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.generator.PurchaseListGenerator;
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.resolver.EventScheduleResolver;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.day.policy.RentPolicy;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
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
import java.math.RoundingMode;
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
    private final WeatherRepository weatherRepository;
    private final OrderRepository orderRepository;
    private final RentPolicy rentPolicy;
    private final PopulationPolicy populationPolicy;
    private final CaptureRatePolicy captureRatePolicy;
    private final EventScheduleResolver eventScheduleResolver;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final PurchaseListGenerator purchaseListGenerator;
    private final Clock clock;

    @Transactional
    public GameDayStartResponse startDay(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        Store store = getActiveStore(user.getId());

        int day = resolveCurrentDay(store.getSeason());
        GameDayLiveState existingState = gameDayStoreStateRedisRepository
                .find(store.getId(), day)
                .orElse(null);
        if (existingState != null && existingState.startResponse() != null) {
            return existingState.startResponse();
        }

        Order existingOrder = orderRepository.findDailyStartOrder(store.getId(), day).orElse(null);
        OpeningState openingState = rentPolicy.resolveStartingState(store, day, existingOrder);

        DaySchedule daySchedule = populationPolicy.buildDaySchedule(store.getLocation().getId(), day);
        Weather weather = selectWeather(day);
        BigDecimal captureRate = captureRatePolicy.resolveStartingCaptureRate(store, day);
        List<GameDayStartResponse.EventSchedule> eventSchedule =
                eventScheduleResolver.resolve(store.getSeason().getId(), day);

        GameDayStartResponse response = new GameDayStartResponse(
                formatHour(BUSINESS_OPEN_HOUR),
                formatHour(BUSINESS_CLOSE_HOUR),
                daySchedule.hourlySchedule(),
                toWeatherLabel(weather.getWeatherType()),
                normalizeScale(weather.getPopulationPercent()),
                daySchedule.dailyTrafficMultiplier(),
                captureRate,
                eventSchedule,
                openingState.initialBalance(),
                openingState.initialStock()
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

    private Weather selectWeather(int day) {
        List<Weather> weathers = weatherRepository.findAllByOrderByIdAsc();
        if (weathers.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return weathers.get(Math.floorMod(day - 1, weathers.size()));
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
                        normalizeCaptureRate(response.captureRate()),
                        store.getPrice(),
                        0,
                        0,
                        0L,
                        0,
                        0,
                        0L,
                        (long) openingState.orderCost(),
                        (long) openingState.initialBalance(),
                        openingState.initialStock(),
                        startedAt
                )
        );
    }

    private String toWeatherLabel(WeatherType weatherType) {
        return weatherType.name();
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCaptureRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatHour(int hour) {
        return LocalTime.of(hour % 24, 0).format(TIME_FORMATTER);
    }
}
