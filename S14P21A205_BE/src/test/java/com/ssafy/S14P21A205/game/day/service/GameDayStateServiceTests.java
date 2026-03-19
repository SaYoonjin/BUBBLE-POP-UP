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
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.policy.CostPolicy;
import com.ssafy.S14P21A205.game.day.resolver.EventEffectResolver;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.day.engine.StockEngine;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventCategory;
import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.entity.OrderType;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.support.GameDayTestFixtures;
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
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private DailyReportRepository dailyReportRepository;

    @Mock
    private ActionLogRepository actionLogRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private GameDayStateService gameDayStateService;

    @BeforeEach
    void setUp() {
        gameDayStateService = createService(Clock.fixed(Instant.parse("2026-03-09T05:32:10Z"), ZoneId.of("Asia/Seoul")));
    }

    private GameDayStateService createService(Clock clock) {
        EventEffectResolver eventEffectResolver = new EventEffectResolver(dailyEventRepository);
        StockEngine stockEngine = new StockEngine();
        PopulationPolicy populationPolicy = new PopulationPolicy(null, null);
        CaptureRatePolicy captureRatePolicy = new CaptureRatePolicy();
        CostPolicy costPolicy = new CostPolicy();
        return new GameDayStateService(
                userService,
                storeRepository,
                actionLogRepository,
                orderRepository,
                eventEffectResolver,
                stockEngine,
                populationPolicy,
                captureRatePolicy,
                costPolicy,
                gameDayStoreStateRedisRepository,
                clock
        );
    }

    @Test
    void getGameStateCalculatesFromRedisState() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 500);
        DailyEvent dailyEvent = dailyEvent(
                store.getSeason(),
                1,
                EventCategory.CELEBRITY_APPEARANCE,
                "연예인 등장",
                "1.50",
                200,
                2,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                40,
                120,
                null,
                null
        );
        DailyEvent ignoredScopedEvent = dailyEvent(
                store.getSeason(),
                1,
                EventCategory.TACO_PRICE_UP,
                "타코 원재료 가격 상승",
                "2.00",
                999,
                7,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                40,
                120,
                null,
                99L
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
        when(storeRepository.countBySeason_IdAndLocation_Id(9L, 3L)).thenReturn(1L);
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(15L, 1)).thenReturn(Optional.empty());
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(15L, 1, OrderType.EMERGENCY))
                .thenReturn(List.of());
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(15L, 1)).thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(9L, 1, 1))
                .thenReturn(List.of(dailyEvent, ignoredScopedEvent));

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(response.serverTime()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(response.lastCalculatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(response.cash()).isEqualTo(3_700L);
        assertThat(response.customerCount()).isEqualTo(6);
        assertThat(response.inventory().totalStock()).isEqualTo(7);
        assertThat(response.population()).isEqualTo("330");
        assertThat(response.appliedEvents()).hasSize(1);
        assertThat(response.appliedEvents().get(0).eventType()).isEqualTo("CELEBRITY_APPEARANCE");
        assertThat(response.appliedEvents().get(0).eventName()).isEqualTo("연예인 등장");

        ArgumentCaptor<GameDayLiveState> stateCaptor = ArgumentCaptor.forClass(GameDayLiveState.class);
        verify(gameDayStoreStateRedisRepository).saveStateAndTickLog(org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq(1), stateCaptor.capture());
        assertThat(stateCaptor.getValue().purchaseCursor()).isEqualTo(6);
        assertThat(stateCaptor.getValue().cumulativeSales()).isEqualTo(2_500L);
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

    @Test
    void refreshGameStateReturnsEmptyOutsidePlayableDayPhase() {
        gameDayStateService = createService(Clock.fixed(Instant.parse("2026-03-09T05:55:00Z"), ZoneId.of("Asia/Seoul")));

        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 500);

        Optional<GameStateResponse> response = gameDayStateService.refreshGameState(store);

        assertThat(response).isEmpty();
    }

    @Test
    void getGameStateAppliesSharedDayFourFixtureBeforeLocationFestival() {
        gameDayStateService = createService(GameDayTestFixtures.fixedClockAt(LocalDateTime.of(2026, 3, 17, 10, 0, 55)));

        User user = GameDayTestFixtures.user(GameDayTestFixtures.USER_ID);
        User dummyUser = GameDayTestFixtures.user(GameDayTestFixtures.DUMMY_USER_ID);
        Location location = GameDayTestFixtures.location();
        Menu menu = GameDayTestFixtures.menu();
        Season season = GameDayTestFixtures.season();
        Store store = GameDayTestFixtures.store(user, season, location, menu);
        Store dummyStore = GameDayTestFixtures.dummyStore(dummyUser, season, location, menu);
        GameDayStartResponse startResponse = GameDayTestFixtures.startResponse(List.of());
        GameDayLiveState state = GameDayTestFixtures.liveState(
                startResponse,
                GameDayTestFixtures.fixedPurchaseList(),
                GameDayTestFixtures.DAY4_STARTED_AT
        );
        List<DailyEvent> dailyEvents = List.of(
                GameDayTestFixtures.globalSupportEvent(season),
                GameDayTestFixtures.menuCostUpEvent(season),
                GameDayTestFixtures.locationFestivalEvent(season)
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(
                GameDayTestFixtures.USER_ID,
                SeasonStatus.IN_PROGRESS
        )).thenReturn(Optional.of(store));
        when(storeRepository.countBySeason_IdAndLocation_Id(GameDayTestFixtures.SEASON_ID, GameDayTestFixtures.LOCATION_ID))
                .thenReturn(2L);
        when(gameDayStoreStateRedisRepository.find(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(GameDayTestFixtures.dailyStartOrder(store)));
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(
                GameDayTestFixtures.STORE_ID,
                GameDayTestFixtures.CURRENT_DAY,
                OrderType.EMERGENCY
        )).thenReturn(List.of());
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                GameDayTestFixtures.SEASON_ID,
                1,
                GameDayTestFixtures.CURRENT_DAY
        )).thenReturn(dailyEvents);

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(dummyStore.getPrice()).isEqualTo(2_000);
        assertThat(response.population()).isEqualTo("76");
        assertThat(response.cash()).isEqualTo(9_800_000L);
        assertThat(response.customerCount()).isEqualTo(40);
        assertThat(response.inventory().totalStock()).isEqualTo(80);
        assertThat(response.appliedEvents()).extracting(GameStateResponse.AppliedEvent::eventType)
                .containsExactly("GOVERNMENT_SUBSIDY", "TACO_PRICE_UP", "FESTIVAL");

        ArgumentCaptor<GameDayLiveState> stateCaptor = ArgumentCaptor.forClass(GameDayLiveState.class);
        verify(gameDayStoreStateRedisRepository).saveStateAndTickLog(
                org.mockito.ArgumentMatchers.eq(GameDayTestFixtures.STORE_ID),
                org.mockito.ArgumentMatchers.eq(GameDayTestFixtures.CURRENT_DAY),
                stateCaptor.capture()
        );
        assertThat(stateCaptor.getValue().purchaseCursor()).isEqualTo(40);
        assertThat(stateCaptor.getValue().tickPurchaseCount()).isEqualTo(10);
        assertThat(stateCaptor.getValue().cumulativeSales()).isEqualTo(200_000L);
        assertThat(stateCaptor.getValue().cumulativeTotalCost()).isEqualTo(300_000L);
    }

    @Test
    void getGameStateAppliesSharedDayFourFixtureAfterLocationFestival() {
        gameDayStateService = createService(GameDayTestFixtures.fixedClockAt(LocalDateTime.of(2026, 3, 17, 10, 1, 5)));

        User user = GameDayTestFixtures.user(GameDayTestFixtures.USER_ID);
        Location location = GameDayTestFixtures.location();
        Menu menu = GameDayTestFixtures.menu();
        Season season = GameDayTestFixtures.season();
        Store store = GameDayTestFixtures.store(user, season, location, menu);
        GameDayLiveState state = GameDayTestFixtures.liveState(
                GameDayTestFixtures.startResponse(List.of()),
                GameDayTestFixtures.fixedPurchaseList(),
                GameDayTestFixtures.DAY4_STARTED_AT
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(
                GameDayTestFixtures.USER_ID,
                SeasonStatus.IN_PROGRESS
        )).thenReturn(Optional.of(store));
        when(storeRepository.countBySeason_IdAndLocation_Id(GameDayTestFixtures.SEASON_ID, GameDayTestFixtures.LOCATION_ID))
                .thenReturn(2L);
        when(gameDayStoreStateRedisRepository.find(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(GameDayTestFixtures.dailyStartOrder(store)));
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(
                GameDayTestFixtures.STORE_ID,
                GameDayTestFixtures.CURRENT_DAY,
                OrderType.EMERGENCY
        )).thenReturn(List.of());
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                GameDayTestFixtures.SEASON_ID,
                1,
                GameDayTestFixtures.CURRENT_DAY
        )).thenReturn(List.of(
                GameDayTestFixtures.globalSupportEvent(season),
                GameDayTestFixtures.menuCostUpEvent(season),
                GameDayTestFixtures.locationFestivalEvent(season)
        ));

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(response.population()).isEqualTo("76");
        assertThat(response.appliedEvents()).extracting(GameStateResponse.AppliedEvent::eventType)
                .containsExactly("GOVERNMENT_SUBSIDY", "TACO_PRICE_UP", "FESTIVAL");
    }

    @Test
    void getGameStateExposesPendingEmergencyOrderBeforeArrival() {
        gameDayStateService = createService(GameDayTestFixtures.fixedClockAt(LocalDateTime.of(2026, 3, 17, 10, 0, 55)));

        User user = GameDayTestFixtures.user(GameDayTestFixtures.USER_ID);
        Location location = GameDayTestFixtures.location();
        Menu menu = GameDayTestFixtures.menu();
        Season season = GameDayTestFixtures.season();
        Store store = GameDayTestFixtures.store(user, season, location, menu);
        GameDayLiveState state = GameDayTestFixtures.liveState(
                GameDayTestFixtures.startResponse(List.of()),
                GameDayTestFixtures.fixedPurchaseList(),
                GameDayTestFixtures.DAY4_STARTED_AT
        );
        Order pendingEmergencyOrder = Order.createEmergency(
                store.getMenu(),
                store,
                20,
                63_000,
                GameDayTestFixtures.CURRENT_DAY,
                LocalDateTime.of(2026, 3, 17, 10, 1, 20)
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(
                GameDayTestFixtures.USER_ID,
                SeasonStatus.IN_PROGRESS
        )).thenReturn(Optional.of(store));
        when(storeRepository.countBySeason_IdAndLocation_Id(GameDayTestFixtures.SEASON_ID, GameDayTestFixtures.LOCATION_ID))
                .thenReturn(2L);
        when(gameDayStoreStateRedisRepository.find(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(GameDayTestFixtures.dailyStartOrder(store)));
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(
                GameDayTestFixtures.STORE_ID,
                GameDayTestFixtures.CURRENT_DAY,
                OrderType.EMERGENCY
        )).thenReturn(List.of(pendingEmergencyOrder));
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                GameDayTestFixtures.SEASON_ID,
                1,
                GameDayTestFixtures.CURRENT_DAY
        )).thenReturn(List.of(
                GameDayTestFixtures.globalSupportEvent(season),
                GameDayTestFixtures.menuCostUpEvent(season)
        ));

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(response.actionStatus().emergencyOrderPending()).isTrue();
        assertThat(response.actionStatus().emergencyOrderArriveAt())
                .isEqualTo(LocalDateTime.of(2026, 3, 17, 10, 1, 20));
    }

    @Test
    void getGameStateAddsEmergencyOrderStockAfterArrival() {
        gameDayStateService = createService(GameDayTestFixtures.fixedClockAt(LocalDateTime.of(2026, 3, 17, 10, 1, 25)));

        User user = GameDayTestFixtures.user(GameDayTestFixtures.USER_ID);
        Location location = GameDayTestFixtures.location();
        Menu menu = GameDayTestFixtures.menu();
        Season season = GameDayTestFixtures.season();
        Store store = GameDayTestFixtures.store(user, season, location, menu);
        GameDayLiveState state = GameDayTestFixtures.liveState(
                GameDayTestFixtures.startResponse(List.of()),
                GameDayTestFixtures.fixedPurchaseList(),
                GameDayTestFixtures.DAY4_STARTED_AT
        );
        Order arrivedEmergencyOrder = Order.createEmergency(
                store.getMenu(),
                store,
                20,
                63_000,
                GameDayTestFixtures.CURRENT_DAY,
                LocalDateTime.of(2026, 3, 17, 10, 1, 20)
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(
                GameDayTestFixtures.USER_ID,
                SeasonStatus.IN_PROGRESS
        )).thenReturn(Optional.of(store));
        when(storeRepository.countBySeason_IdAndLocation_Id(GameDayTestFixtures.SEASON_ID, GameDayTestFixtures.LOCATION_ID))
                .thenReturn(2L);
        when(gameDayStoreStateRedisRepository.find(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(state));
        when(orderRepository.findDailyStartOrder(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(Optional.of(GameDayTestFixtures.dailyStartOrder(store)));
        when(orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(
                GameDayTestFixtures.STORE_ID,
                GameDayTestFixtures.CURRENT_DAY,
                OrderType.EMERGENCY
        )).thenReturn(List.of(arrivedEmergencyOrder));
        when(actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(GameDayTestFixtures.STORE_ID, GameDayTestFixtures.CURRENT_DAY))
                .thenReturn(List.of());
        when(dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                GameDayTestFixtures.SEASON_ID,
                1,
                GameDayTestFixtures.CURRENT_DAY
        )).thenReturn(List.of(
                GameDayTestFixtures.globalSupportEvent(season),
                GameDayTestFixtures.menuCostUpEvent(season)
        ));

        GameStateResponse response = gameDayStateService.getGameState(mock(Authentication.class));

        assertThat(response.actionStatus().emergencyOrderPending()).isFalse();
        assertThat(response.inventory().totalStock()).isEqualTo(90);
        assertThat(response.cash()).isEqualTo(9_777_000L);
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
                        new BigDecimal("0.10"),
                        List.of(),
                        initialBalance,
                        initialStock,
                        null,
                        null
                ),
                0,
                0,
                new BigDecimal("0.10"),
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
        hourlySchedule.put("10", new GameDayStartResponse.HourlySchedule(100, BigDecimal.ONE, BigDecimal.ONE, 110));
        hourlySchedule.put("11", new GameDayStartResponse.HourlySchedule(200, BigDecimal.ONE, BigDecimal.ONE, 220));
        hourlySchedule.put("12", new GameDayStartResponse.HourlySchedule(300, BigDecimal.ONE, BigDecimal.ONE, 330));
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
        LocalDateTime currentBusinessAt = LocalDateTime.of(2026, 3, 9, 14, 32, 10);
        LocalDateTime seasonStartAt = currentBusinessAt.minusSeconds(120L + (currentDay - 1L) * 180L + 50L + 60L);
        ReflectionTestUtils.setField(season, "startTime", seasonStartAt);
        ReflectionTestUtils.setField(season, "endTime", seasonStartAt.plusSeconds(120L + totalDays * 180L + 120L));

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
            EventCategory eventCategory,
            String eventName,
            String populationRate,
            int capitalFlat,
            int stockFlat,
            EventStartTime startTime,
            EventEndTime endTime,
            Integer applyOffsetSeconds,
            Integer expireOffsetSeconds,
            Long targetLocationId,
            Long targetMenuId
    ) {
        RandomEvent randomEvent = instantiate(RandomEvent.class);
        ReflectionTestUtils.setField(randomEvent, "id", 2L);
        ReflectionTestUtils.setField(randomEvent, "eventCategory", eventCategory);
        ReflectionTestUtils.setField(randomEvent, "eventName", eventName);
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
        ReflectionTestUtils.setField(dailyEvent, "targetLocationId", targetLocationId);
        ReflectionTestUtils.setField(dailyEvent, "targetMenuId", targetMenuId);
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


