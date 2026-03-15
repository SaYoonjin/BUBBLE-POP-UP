package com.ssafy.S14P21A205.game.day.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GameDayStartServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private PopulationRepository populationRepository;

    @Mock
    private TrafficRepository trafficRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private DailyEventRepository dailyEventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    private GameDayStartService gameDayStartService;

    @BeforeEach
    void setUp() {
        gameDayStartService = new GameDayStartService(
                userService,
                storeRepository,
                dailyReportRepository,
                populationRepository,
                trafficRepository,
                weatherRepository,
                dailyEventRepository,
                orderRepository,
                gameDayStoreStateRedisRepository
        );
        ReflectionTestUtils.setField(
                gameDayStartService,
                "clock",
                Clock.fixed(Instant.parse("2026-03-09T05:32:10Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void startDayCreatesInitialStateAndReturnsResponse() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 4_500, 100_000, 2_000);
        Weather sunny = weather(WeatherType.SUNNY, "1.10");
        DailyEvent dailyEvent = dailyEvent(store.getSeason(), 1, "celebrity", "1.15", 200_000);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrder(15L, 1)).thenReturn(Optional.empty());
        when(populationRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), 500),
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 11, 0), 650),
                population(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), 900)
        ));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), 100),
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 11, 0), 80),
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), 120)
        ));
        when(weatherRepository.findAllByOrderByIdAsc()).thenReturn(List.of(sunny));
        when(dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(9L, 1)).thenReturn(List.of(dailyEvent));

        GameDayStartResponse response = gameDayStartService.startDay(mock(Authentication.class));

        assertThat(response.startTime()).isEqualTo("10:00");
        assertThat(response.endTime()).isEqualTo("22:00");
        assertThat(response.weatherMultiplier()).isEqualByComparingTo("1.10");
        assertThat(response.captureRate()).isEqualByComparingTo("0.00");
        assertThat(response.hourlySchedule().get("10").population()).isEqualTo(500);
        assertThat(response.hourlySchedule().get("11").trafficMultiplier()).isEqualByComparingTo("0.80");
        assertThat(response.initialStock()).isEqualTo(0);
        assertThat(response.initialBalance()).isEqualTo(9_900_000);
        assertThat(response.eventSchedule()).hasSize(1);

        ArgumentCaptor<GameDayLiveState> stateCaptor = ArgumentCaptor.forClass(GameDayLiveState.class);
        verify(gameDayStoreStateRedisRepository).save(org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq(1), stateCaptor.capture());
        assertThat(stateCaptor.getValue().purchaseCursor()).isZero();
        assertThat(stateCaptor.getValue().purchaseList()).hasSize(1_150);
        assertThat(stateCaptor.getValue().startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(stateCaptor.getValue().startResponse()).isEqualTo(response);
    }

    @Test
    void startDayReturnsExistingStateResponseWhenSameDayAlreadyStarted() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 5_000, 100_000, 2_000);
        GameDayStartResponse existingResponse = new GameDayStartResponse(
                "10:00",
                "22:00",
                Map.of(),
                "SUNNY",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(),
                9_100_000,
                100
        );
        GameDayLiveState existingState = new GameDayLiveState(
                LocalDateTime.of(2026, 3, 9, 14, 30, 0),
                List.of(1, 2, 3),
                0,
                existingResponse,
                0,
                0,
                BigDecimal.ZERO,
                5_000,
                0,
                0,
                0L,
                0,
                0,
                0L,
                0L,
                9_100_000L,
                100,
                LocalDateTime.of(2026, 3, 9, 14, 30, 0)
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.of(existingState));

        GameDayStartResponse response = gameDayStartService.startDay(mock(Authentication.class));

        assertThat(response).isEqualTo(existingResponse);
        verify(gameDayStoreStateRedisRepository, never()).save(any(), any(), any());
    }

    @Test
    void startDayThrowsWhenPreviousReportIsMissingOnLaterDay() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 2, 7, 5_000, 100_000, 2_000);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 2)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrder(15L, 2)).thenReturn(Optional.empty());
        when(dailyReportRepository.findByStoreIdAndDay(15L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameDayStartService.startDay(mock(Authentication.class)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                });
    }

    private User user(int id) {
        User user = new User("test@example.com", "tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Store store(
            User user,
            Long storeId,
            Long locationId,
            Long menuId,
            Long seasonId,
            int currentDay,
            int totalDays,
            int price,
            int rent,
            int originPrice
    ) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", locationId);
        ReflectionTestUtils.setField(location, "rent", rent);

        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", menuId);
        ReflectionTestUtils.setField(menu, "originPrice", originPrice);

        Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", seasonId);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(season, "currentDay", currentDay);
        ReflectionTestUtils.setField(season, "totalDays", totalDays);

        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "user", user);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "menu", menu);
        ReflectionTestUtils.setField(store, "season", season);
        ReflectionTestUtils.setField(store, "price", price);
        return store;
    }

    private Weather weather(WeatherType weatherType, String populationPercent) {
        Weather weather = instantiate(Weather.class);
        ReflectionTestUtils.setField(weather, "id", 1L);
        ReflectionTestUtils.setField(weather, "weatherType", weatherType);
        ReflectionTestUtils.setField(weather, "populationPercent", new BigDecimal(populationPercent));
        return weather;
    }

    private DailyEvent dailyEvent(Season season, int day, String eventType, String populationRate, int capitalFlat) {
        RandomEvent randomEvent = instantiate(RandomEvent.class);
        ReflectionTestUtils.setField(randomEvent, "id", 2L);
        ReflectionTestUtils.setField(randomEvent, "eventType", eventType);
        ReflectionTestUtils.setField(randomEvent, "startTime", EventStartTime.IMMEDIATE);
        ReflectionTestUtils.setField(randomEvent, "endTime", EventEndTime.SAME_DAY);
        ReflectionTestUtils.setField(randomEvent, "populationRate", new BigDecimal(populationRate));
        ReflectionTestUtils.setField(randomEvent, "capitalFlat", capitalFlat);

        DailyEvent dailyEvent = instantiate(DailyEvent.class);
        ReflectionTestUtils.setField(dailyEvent, "id", 3L);
        ReflectionTestUtils.setField(dailyEvent, "season", season);
        ReflectionTestUtils.setField(dailyEvent, "event", randomEvent);
        ReflectionTestUtils.setField(dailyEvent, "day", day);
        ReflectionTestUtils.setField(dailyEvent, "applyOffsetSeconds", 40);
        ReflectionTestUtils.setField(dailyEvent, "expireOffsetSeconds", 120);
        ReflectionTestUtils.setField(dailyEvent, "newsTitle", eventType.toLowerCase());
        return dailyEvent;
    }

    private Population population(Location location, LocalDateTime dateTime, int floatingPopulation) {
        Population population = instantiate(Population.class);
        ReflectionTestUtils.setField(population, "location", location);
        ReflectionTestUtils.setField(population, "date", dateTime);
        ReflectionTestUtils.setField(population, "floatingPopulation", floatingPopulation);
        return population;
    }

    private Traffic traffic(Location location, LocalDateTime dateTime, int trafficStatus) {
        Traffic traffic = instantiate(Traffic.class);
        ReflectionTestUtils.setField(traffic, "location", location);
        ReflectionTestUtils.setField(traffic, "date", dateTime);
        ReflectionTestUtils.setField(traffic, "trafficStatus", trafficStatus);
        return traffic;
    }

    private DailyReport dailyReport(int balance, int stockRemaining, String captureRate) {
        DailyReport dailyReport = instantiate(DailyReport.class);
        ReflectionTestUtils.setField(dailyReport, "balance", balance);
        ReflectionTestUtils.setField(dailyReport, "stockRemaining", stockRemaining);
        ReflectionTestUtils.setField(dailyReport, "captureRate", new BigDecimal(captureRate));
        return dailyReport;
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
