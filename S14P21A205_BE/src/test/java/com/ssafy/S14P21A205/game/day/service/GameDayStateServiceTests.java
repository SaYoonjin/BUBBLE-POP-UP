package com.ssafy.S14P21A205.game.day.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventCategory;
import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.order.entity.OrderType;
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
import java.util.LinkedHashMap;
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
class GameDayStateServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private DailyEventRepository dailyEventRepository;

    @Mock
    private ActionLogRepository actionLogRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    private GameDayStateService gameDayStateService;

    @BeforeEach
    void setUp() {
        gameDayStateService = new GameDayStateService(
                userService,
                storeRepository,
                dailyEventRepository,
                actionLogRepository,
                orderRepository,
                gameDayStoreStateRedisRepository
        );
        ReflectionTestUtils.setField(
                gameDayStateService,
                "clock",
                Clock.fixed(Instant.parse("2026-03-09T05:32:10Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void getGameStateCalculatesFromRedisState() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 500);
        DailyEvent dailyEvent = dailyEvent(
                store.getSeason(),
                1,
                "CELEBRITY",
                "1.50",
                200,
                2,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                40,
                120
        );
        GameDayLiveState state = state(
                500,
                List.of(1, 0, 2, 1, 1, 0),
                1_000,
                10,
                LocalDateTime.of(2026, 3, 9, 14, 30, 0)
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(15L, 1)).thenReturn(Optional.empty());
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(15L, 1, OrderType.EMERGENCY))
                .thenReturn(List.of());
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(15L, 1)).thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(9L, 1, 1)).thenReturn(List.of(dailyEvent));

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(response.serverTime()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(response.lastCalculatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(response.cash()).isEqualTo(3_200L);
        assertThat(response.customerCount()).isEqualTo(4);
        assertThat(response.inventory().totalStock()).isEqualTo(8);
        assertThat(response.population()).isEqualTo("495");
        assertThat(response.appliedEvents()).hasSize(1);
        assertThat(response.appliedEvents().get(0).eventType()).isEqualTo("CELEBRITY");

        ArgumentCaptor<GameDayLiveState> stateCaptor = ArgumentCaptor.forClass(GameDayLiveState.class);
        verify(gameDayStoreStateRedisRepository).saveStateAndTickLog(org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq(1), stateCaptor.capture());
        assertThat(stateCaptor.getValue().purchaseCursor()).isEqualTo(4);
        assertThat(stateCaptor.getValue().cumulativeSales()).isEqualTo(2_000L);
        assertThat(stateCaptor.getValue().cumulativeTotalCost()).isEqualTo(300L);
    }

    @Test
    void getGameStateThrowsWhenStateIsMissing() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 500);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameDayStateService.getGameState(mock(Authentication.class)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                });
    }

    private GameDayLiveState state(
            int salePrice,
            List<Integer> purchaseList,
            int initialBalance,
            int initialStock,
            LocalDateTime startedAt
    ) {
        return new GameDayLiveState(
                startedAt,
                purchaseList,
                0,
                new GameDayStartResponse(
                        "10:00",
                        "22:00",
                        hourlySchedule(),
                        "SUNNY",
                        new BigDecimal("1.10"),
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        List.of(),
                        initialBalance,
                        initialStock
                ),
                0,
                0,
                BigDecimal.ZERO,
                salePrice,
                0,
                0,
                0L,
                0,
                0,
                0L,
                0L,
                (long) initialBalance,
                initialStock,
                startedAt
        );
    }

    private Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule() {
        Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule = new LinkedHashMap<>();
        hourlySchedule.put("10", new GameDayStartResponse.HourlySchedule(100, BigDecimal.ONE));
        hourlySchedule.put("11", new GameDayStartResponse.HourlySchedule(200, BigDecimal.ONE));
        hourlySchedule.put("12", new GameDayStartResponse.HourlySchedule(300, BigDecimal.ONE));
        return hourlySchedule;
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
            int price
    ) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", locationId);
        ReflectionTestUtils.setField(location, "rent", 300);

        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", menuId);

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

    private DailyEvent dailyEvent(
            Season season,
            int day,
            String eventType,
            String populationRate,
            int capitalFlat,
            int stockFlat,
            EventStartTime startTime,
            EventEndTime endTime,
            Integer applyOffsetSeconds,
            Integer expireOffsetSeconds
    ) {
        RandomEvent randomEvent = instantiate(RandomEvent.class);
        ReflectionTestUtils.setField(randomEvent, "id", 2L);
        ReflectionTestUtils.setField(randomEvent, "eventCategory", EventCategory.GOOD);
        ReflectionTestUtils.setField(randomEvent, "eventType", eventType);
        ReflectionTestUtils.setField(randomEvent, "startTime", startTime);
        ReflectionTestUtils.setField(randomEvent, "endTime", endTime);
        ReflectionTestUtils.setField(randomEvent, "populationRate", new BigDecimal(populationRate));
        ReflectionTestUtils.setField(randomEvent, "stockFlat", BigDecimal.valueOf(stockFlat));
        ReflectionTestUtils.setField(randomEvent, "capitalFlat", capitalFlat);

        DailyEvent dailyEvent = instantiate(DailyEvent.class);
        ReflectionTestUtils.setField(dailyEvent, "id", 3L);
        ReflectionTestUtils.setField(dailyEvent, "season", season);
        ReflectionTestUtils.setField(dailyEvent, "event", randomEvent);
        ReflectionTestUtils.setField(dailyEvent, "day", day);
        ReflectionTestUtils.setField(dailyEvent, "applyOffsetSeconds", applyOffsetSeconds);
        ReflectionTestUtils.setField(dailyEvent, "expireOffsetSeconds", expireOffsetSeconds);
        ReflectionTestUtils.setField(dailyEvent, "newsTitle", eventType);
        return dailyEvent;
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
