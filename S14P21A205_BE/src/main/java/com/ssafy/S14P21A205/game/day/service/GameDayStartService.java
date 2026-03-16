package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStartService {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final int BUSINESS_OPEN_HOUR = SeasonTimeline.BUSINESS_OPEN_HOUR;
    private static final int BUSINESS_CLOSE_HOUR = SeasonTimeline.BUSINESS_CLOSE_HOUR;
    private static final int[] PURCHASE_QUANTITY_WEIGHTS = {10, 40, 35, 15};
    private static final BigDecimal INITIAL_CAPTURE_RATE = new BigDecimal("0.00");
    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final SeasonTimeline SEASON_TIMELINE_POLICY = new SeasonTimeline();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyReportRepository dailyReportRepository;
    private final PopulationRepository populationRepository;
    private final TrafficRepository trafficRepository;
    private final WeatherRepository weatherRepository;
    private final DailyEventRepository dailyEventRepository;
    private final OrderRepository orderRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    private Clock clock = Clock.systemDefaultZone();

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
        StartingState startingState = resolveStartingState(store, day, existingOrder);

        DaySchedule daySchedule = buildDaySchedule(store.getLocation().getId(), day);
        Weather weather = selectWeather(day);
        BigDecimal captureRate = resolveStartingCaptureRate(store, day);
        List<GameDayStartResponse.EventSchedule> eventSchedule = buildEventSchedule(store.getSeason().getId(), day);

        GameDayStartResponse response = new GameDayStartResponse(
                formatHour(BUSINESS_OPEN_HOUR),
                formatHour(BUSINESS_CLOSE_HOUR),
                daySchedule.hourlySchedule(),
                toWeatherLabel(weather.getWeatherType()),
                normalizeScale(weather.getPopulationPercent()),
                daySchedule.dailyTrafficMultiplier(),
                captureRate,
                eventSchedule,
                startingState.initialBalance(),
                startingState.initialStock()
        );

        writeInitialState(store, day, startingState, response);
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

    private StartingState resolveStartingState(Store store, int day, Order existingOrder) {
        int orderCount = existingOrder == null ? 0 : existingOrder.getQuantity();
        int orderCost = existingOrder == null ? 0 : existingOrder.getTotalCost();

        int carriedBalance;
        int carriedStock;
        if (day == 1) {
            carriedBalance = INITIAL_CAPITAL;
            carriedStock = 0;
        } else {
            DailyReport previousDay = dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
            carriedBalance = previousDay.getBalance();
            carriedStock = previousDay.getStockRemaining();
        }

        int dailyRent = store.getLocation().getRent();
        int balanceAfterDailyRent = carriedBalance - dailyRent;
        int initialBalance = balanceAfterDailyRent - orderCost;
        if (initialBalance < 0) {
            int maxAffordableOrderCount = Math.max(0, balanceAfterDailyRent / store.getMenu().getOriginPrice());
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Insufficient balance for today's fixed costs. "
                            + "maxOrderCount=%d, existingOrderCount=%d, balanceBeforeOrder=%d, dailyRent=%d, originPrice=%d"
                            .formatted(
                                    maxAffordableOrderCount,
                                    orderCount,
                                    carriedBalance,
                                    dailyRent,
                                    store.getMenu().getOriginPrice()
                            )
            );
        }

        return new StartingState(initialBalance, Math.addExact(carriedStock, orderCount), orderCount, orderCost);
    }

    private DaySchedule buildDaySchedule(Long locationId, int day) {
        List<Population> populations = populationRepository.findByLocationIdOrderByDateAsc(locationId);
        List<Traffic> traffics = trafficRepository.findByLocationIdOrderByDateAsc(locationId);
        if (populations.isEmpty() || traffics.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        List<Population> selectedPopulations = selectRowsForDay(populations, Population::getDate, day);
        List<Traffic> selectedTraffics = selectRowsForDay(traffics, Traffic::getDate, day);
        BigDecimal trafficBaseline = averageTrafficStatus(traffics);

        Map<Integer, Integer> populationByHour = new LinkedHashMap<>();
        for (Population population : selectedPopulations) {
            int hour = population.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                populationByHour.put(hour, population.getFloatingPopulation());
            }
        }

        Map<Integer, Integer> trafficByHour = new LinkedHashMap<>();
        for (Traffic traffic : selectedTraffics) {
            int hour = traffic.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                trafficByHour.put(hour, traffic.getTrafficStatus().getValue());
            }
        }

        LinkedHashMap<String, GameDayStartResponse.HourlySchedule> hourlySchedule = new LinkedHashMap<>();
        List<BigDecimal> hourlyMultipliers = new ArrayList<>();
        for (int hour = BUSINESS_OPEN_HOUR; hour < BUSINESS_CLOSE_HOUR; hour++) {
            BigDecimal trafficMultiplier = trafficByHour.containsKey(hour)
                    ? ratio(trafficByHour.get(hour), trafficBaseline)
                    : DECIMAL_ONE;
            hourlySchedule.put(
                    String.valueOf(hour),
                    new GameDayStartResponse.HourlySchedule(
                            populationByHour.getOrDefault(hour, 0),
                            trafficMultiplier
                    )
            );
            hourlyMultipliers.add(trafficMultiplier);
        }

        return new DaySchedule(hourlySchedule, average(hourlyMultipliers));
    }

    private <T> List<T> selectRowsForDay(List<T> rows, Function<T, LocalDateTime> dateExtractor, int day) {
        Map<LocalDate, List<T>> rowsByDate = new LinkedHashMap<>();
        for (T row : rows) {
            rowsByDate.computeIfAbsent(dateExtractor.apply(row).toLocalDate(), key -> new ArrayList<>()).add(row);
        }

        List<List<T>> groupedRows = new ArrayList<>(rowsByDate.values());
        if (groupedRows.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        int index = Math.floorMod(day - 1, groupedRows.size());
        return groupedRows.get(index);
    }

    private BigDecimal averageTrafficStatus(List<Traffic> traffics) {
        BigDecimal total = BigDecimal.ZERO;
        for (Traffic traffic : traffics) {
            total = total.add(BigDecimal.valueOf(traffic.getTrafficStatus().getValue()));
        }
        return total.divide(BigDecimal.valueOf(traffics.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(int trafficStatus, BigDecimal baseline) {
        if (baseline.compareTo(BigDecimal.ZERO) == 0) {
            return DECIMAL_ONE;
        }
        return BigDecimal.valueOf(trafficStatus).divide(baseline, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return DECIMAL_ONE;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveStartingCaptureRate(Store store, int day) {
        if (day == 1) {
            return INITIAL_CAPTURE_RATE;
        }

        DailyReport previousDay = dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
        return normalizeScale(previousDay.getCaptureRate());
    }

    private Weather selectWeather(int day) {
        List<Weather> weathers = weatherRepository.findAllByOrderByIdAsc();
        if (weathers.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return weathers.get(Math.floorMod(day - 1, weathers.size()));
    }

    private List<GameDayStartResponse.EventSchedule> buildEventSchedule(Long seasonId, int day) {
        List<DailyEvent> dailyEvents = dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(seasonId, day);
        List<GameDayStartResponse.EventSchedule> eventSchedule = new ArrayList<>();
        for (DailyEvent dailyEvent : dailyEvents) {
            RandomEvent event = dailyEvent.getEvent();
            Integer balanceChange = event.getCapitalFlat() == null || event.getCapitalFlat() == 0
                    ? null
                    : event.getCapitalFlat();
            eventSchedule.add(new GameDayStartResponse.EventSchedule(
                    SEASON_TIMELINE_POLICY.formatGameTime(dailyEvent.getApplyOffsetSeconds()),
                    event.getEventType(),
                    new GameDayStartResponse.Scope(null, null),
                    dailyEvent.getNewsTitle(),
                    normalizeScale(event.getPopulationRate()),
                    balanceChange
            ));
        }
        return eventSchedule;
    }

    private void writeInitialState(
            Store store,
            int day,
            StartingState startingState,
            GameDayStartResponse response
    ) {
        List<Integer> purchaseList = buildPurchaseList(response.hourlySchedule());
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
                        (long) startingState.orderCost(),
                        (long) startingState.initialBalance(),
                        startingState.initialStock(),
                        startedAt
                )
        );
    }

    private List<Integer> buildPurchaseList(Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule) {
        int expectedCustomerCount = 0;
        for (GameDayStartResponse.HourlySchedule schedule : hourlySchedule.values()) {
            expectedCustomerCount += schedule.population();
        }

        List<Integer> purchaseList = new ArrayList<>(expectedCustomerCount);
        for (int i = 0; i < expectedCustomerCount; i++) {
            purchaseList.add(drawPurchaseQuantity(ThreadLocalRandom.current().nextInt(100)));
        }
        return purchaseList;
    }

    private int drawPurchaseQuantity(int roll) {
        int cumulative = 0;
        for (int quantity = 0; quantity < PURCHASE_QUANTITY_WEIGHTS.length; quantity++) {
            cumulative += PURCHASE_QUANTITY_WEIGHTS[quantity];
            if (roll < cumulative) {
                return quantity;
            }
        }
        return PURCHASE_QUANTITY_WEIGHTS.length - 1;
    }

    private String toWeatherLabel(WeatherType weatherType) {
        return switch (weatherType) {
            case SUNNY -> "맑음";
            case RAIN -> "비";
            case SNOW -> "눈";
            case HEATWAVE -> "폭염";
            case COLDWAVE -> "한파";
            case FOG -> "흐림";
        };
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

    private record StartingState(
            int initialBalance,
            int initialStock,
            int orderCount,
            int orderCost
    ) {
    }

    private record DaySchedule(
            Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule,
            BigDecimal dailyTrafficMultiplier
    ) {
    }
}
