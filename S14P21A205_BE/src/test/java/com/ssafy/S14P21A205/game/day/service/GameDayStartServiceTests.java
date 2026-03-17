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
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.generator.PurchaseListGenerator;
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.day.policy.RentPolicy;
import com.ssafy.S14P21A205.game.day.policy.StoreRankingPolicy;
import com.ssafy.S14P21A205.game.day.resolver.EnvironmentScheduleResolver;
import com.ssafy.S14P21A205.game.day.resolver.EventScheduleResolver;
import com.ssafy.S14P21A205.game.day.resolver.NewsRankingResolver;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import com.ssafy.S14P21A205.game.environment.repository.FestivalRepository;
import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.environment.repository.SeasonWeatherRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.news.entity.NewsReport;
import com.ssafy.S14P21A205.game.news.repository.NewsReportRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

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
    private FestivalRepository festivalRepository;

    @Mock
    private DailyEventRepository dailyEventRepository;

    @Mock
    private NewsReportRepository newsReportRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    @Mock
    private PurchaseListGenerator purchaseListGenerator;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ItemUserRepository itemUserRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private GameDayStartService gameDayStartService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-09T05:32:10Z"), ZoneId.of("Asia/Seoul"));
        gameDayStartService = createService(fixedClock);
        org.mockito.Mockito.lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.lenient().when(valueOperations.get(any())).thenReturn(null);
        org.mockito.Mockito.lenient()
                .when(itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(1, ItemCategory.RENT))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(1, ItemCategory.INGREDIENT))
                .thenReturn(Optional.empty());
    }

    private GameDayStartService createService(Clock clock) {
        StoreRankingPolicy storeRankingPolicy = new StoreRankingPolicy();
        RentPolicy rentPolicy = new RentPolicy(dailyReportRepository, stringRedisTemplate, itemUserRepository, storeRankingPolicy);
        PopulationPolicy populationPolicy = new PopulationPolicy(populationRepository, trafficRepository);
        CaptureRatePolicy captureRatePolicy = new CaptureRatePolicy();
        ObjectMapper objectMapper = new ObjectMapper();
        SeasonWeatherRedisRepository seasonWeatherRedisRepository =
                new SeasonWeatherRedisRepository(stringRedisTemplate, objectMapper);
        EnvironmentScheduleResolver environmentScheduleResolver =
                new EnvironmentScheduleResolver(populationPolicy, seasonWeatherRedisRepository);
        NewsRankingResolver newsRankingResolver = new NewsRankingResolver(newsReportRepository, objectMapper);
        EventScheduleResolver eventScheduleResolver = new EventScheduleResolver(dailyEventRepository);
        return new GameDayStartService(
                userService,
                storeRepository,
                environmentScheduleResolver,
                festivalRepository,
                orderRepository,
                rentPolicy,
                captureRatePolicy,
                storeRankingPolicy,
                newsRankingResolver,
                eventScheduleResolver,
                gameDayStoreStateRedisRepository,
                purchaseListGenerator,
                clock
        );
    }

    @Test
    void startDayCreatesInitialStateAndFiltersScopedEvents() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 4_500, 100_000, 2_000);
        DailyEvent globalEvent = dailyEvent(store.getSeason(), 1, "celebrity", "1.15", 200_000, null, null);
        DailyEvent locationEvent = dailyEvent(store.getSeason(), 1, "local-festival", "1.05", 0, 3L, null);
        DailyEvent ignoredMenuEvent = dailyEvent(store.getSeason(), 1, "other-menu-sale", "1.50", 999_999, null, 99L);
        List<Integer> fixedPurchaseList = List.of(1, 0, 2, 1, 1);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrders(15L, 1)).thenReturn(List.of());
        when(valueOperations.get("season:9:weather_schedule")).thenReturn(
                """
                [
                  {"day":1,"weatherType":"SUNNY","populationMultiplier":1.10},
                  {"day":2,"weatherType":"RAIN","populationMultiplier":0.90},
                  {"day":3,"weatherType":"SNOW","populationMultiplier":0.80},
                  {"day":4,"weatherType":"HEATWAVE","populationMultiplier":0.90},
                  {"day":5,"weatherType":"FOG","populationMultiplier":0.95},
                  {"day":6,"weatherType":"COLDWAVE","populationMultiplier":0.90},
                  {"day":7,"weatherType":"SUNNY","populationMultiplier":1.10}
                ]
                """
        );
        when(populationRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), 500),
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 11, 0), 650),
                population(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), 900)
        ));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), TrafficStatus.CONGESTED),
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 11, 0), TrafficStatus.NORMAL),
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), TrafficStatus.VERY_CONGESTED)
        ));
        when(dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(9L, 1))
                .thenReturn(List.of(globalEvent, locationEvent, ignoredMenuEvent));
        when(purchaseListGenerator.generate(any())).thenReturn(fixedPurchaseList);

        GameDayStartResponse response = gameDayStartService.startDay(mock(Authentication.class));

        assertThat(response.startTime()).isEqualTo("10:00");
        assertThat(response.endTime()).isEqualTo("22:00");
        assertThat(response.weatherMultiplier()).isEqualByComparingTo("1.10");
        assertThat(response.captureRate()).isEqualByComparingTo("0.12");
        assertThat(response.hourlySchedule().get("10").population()).isEqualTo(500);
        assertThat(response.hourlySchedule().get("11").trafficMultiplier()).isEqualByComparingTo("0.75");
        assertThat(response.hourlySchedule().get("10").effectivePopulation()).isEqualTo(550);
        assertThat(response.hourlySchedule().get("11").effectivePopulation()).isEqualTo(536);
        assertThat(response.initialStock()).isEqualTo(0);
        assertThat(response.initialBalance()).isEqualTo(9_670_000);
        assertThat(response.eventSchedule()).hasSize(2);
        assertThat(response.eventSchedule().get(0).type()).isEqualTo("celebrity");
        assertThat(response.eventSchedule().get(0).scope()).isNull();
        assertThat(response.eventSchedule().get(1).type()).isEqualTo("local-festival");
        assertThat(response.eventSchedule().get(1).scope()).isNotNull();
        assertThat(response.eventSchedule().get(1).scope().region()).isEqualTo(3L);
        assertThat(response.eventSchedule().get(1).scope().menu()).isNull();
        assertThat(response.marketSnapshot().avgMenuPrice()).isEqualTo(4_500);
        assertThat(response.marketSnapshot().regionStoreCount()).isEqualTo(1);
        assertThat(response.openingSummary().dailyRentApplied()).isEqualTo(130_000);
        assertThat(response.openingSummary().interiorCost()).isEqualTo(200_000);
        assertThat(response.openingSummary().appliedUnitCost()).isEqualTo(2_400);

        ArgumentCaptor<GameDayLiveState> stateCaptor = ArgumentCaptor.forClass(GameDayLiveState.class);
        verify(gameDayStoreStateRedisRepository).save(org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq(1), stateCaptor.capture());
        assertThat(stateCaptor.getValue().purchaseCursor()).isZero();
        assertThat(stateCaptor.getValue().purchaseList()).isEqualTo(fixedPurchaseList);
        assertThat(stateCaptor.getValue().startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 14, 32, 10));
        assertThat(stateCaptor.getValue().startResponse()).isEqualTo(response);
        assertThat(stateCaptor.getValue().cumulativeTotalCost()).isEqualTo(330_000L);
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
                100,
                null,
                null
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
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 2)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrders(15L, 2)).thenReturn(List.of());
        when(valueOperations.get("season:9:weather_schedule")).thenReturn(
                """
                [
                  {"day":2,"weatherType":"SUNNY","populationMultiplier":1.00}
                ]
                """
        );
        when(populationRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), 500)
        ));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), TrafficStatus.NORMAL)
        ));
        when(dailyReportRepository.findByStoreIdAndDay(15L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameDayStartService.startDay(mock(Authentication.class)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void startDayUsesPersistedBalanceAndSeasonAdjustedRegularOrderCost() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 4_500, 100_000, 2_000);
        Order existingOrder = Order.create(store.getMenu(), store, 120, 1_000_000, 1);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrders(15L, 1)).thenReturn(List.of(existingOrder));
        when(valueOperations.get("season:9:weather_schedule")).thenReturn(
                """
                [
                  {"day":1,"weatherType":"SUNNY","populationMultiplier":1.00}
                ]
                """
        );
        when(valueOperations.get("balance:15")).thenReturn("8900000");
        when(populationRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                population(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), 500)
        ));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 1, 10, 0), TrafficStatus.NORMAL)
        ));
        when(dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(9L, 1)).thenReturn(List.of());
        when(purchaseListGenerator.generate(any())).thenReturn(List.of(1, 1, 1));

        GameDayStartResponse response = gameDayStartService.startDay(mock(Authentication.class));

        assertThat(response.initialBalance()).isEqualTo(8_282_000);
        assertThat(response.initialStock()).isEqualTo(120);
        assertThat(response.openingSummary().regularOrderCost()).isEqualTo(288_000);
    }

    @Test
    void startDayUsesPreviousNewsReportRanksForRentAndIngredientCost() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 2, 7, 4_500, 100_000, 2_000);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 2)).thenReturn(Optional.empty());
        when(orderRepository.findDailyStartOrders(15L, 2)).thenReturn(List.of());
        when(valueOperations.get("season:9:weather_schedule")).thenReturn(
                """
                [
                  {"day":2,"weatherType":"RAIN","populationMultiplier":0.90}
                ]
                """
        );
        when(populationRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                population(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), 400)
        ));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L)).thenReturn(List.of(
                traffic(store.getLocation(), LocalDateTime.of(2026, 3, 2, 10, 0), TrafficStatus.NORMAL)
        ));
        when(dailyReportRepository.findByStoreIdAndDay(15L, 1)).thenReturn(Optional.of(previousDailyReport(store, 9_000_000, 10)));
        when(newsReportRepository.findFirstBySeason_IdAndDay(9L, 1)).thenReturn(Optional.of(newsReport(
                store.getSeason(),
                1,
                """
                [{"locationId":3,"rank":2}]
                """,
                """
                [{"menuId":1,"rank":2}]
                """
        )));
        when(dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(9L, 2)).thenReturn(List.of());
        when(purchaseListGenerator.generate(any())).thenReturn(List.of(1, 1, 1));

        GameDayStartResponse response = gameDayStartService.startDay(mock(Authentication.class));

        assertThat(response.marketSnapshot().locationPopularityRank()).isEqualTo(2);
        assertThat(response.marketSnapshot().menuTrendRank()).isEqualTo(2);
        assertThat(response.openingSummary().dailyRentApplied()).isEqualTo(120_000);
        assertThat(response.openingSummary().interiorCost()).isZero();
        assertThat(response.openingSummary().appliedUnitCost()).isEqualTo(2_200);
        assertThat(response.openingSummary().trendCostMultiplier()).isEqualByComparingTo("1.10");
        assertThat(response.initialBalance()).isEqualTo(8_880_000);
    }

    @Test
    void startDayThrowsWhenWeatherScheduleIsMissingInRedis() {
        User user = user(1);
        Store store = store(user, 15L, 3L, 1L, 9L, 1, 7, 4_500, 100_000, 2_000);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.find(15L, 1)).thenReturn(Optional.empty());
        when(valueOperations.get("season:9:weather_schedule")).thenReturn(null);

        assertThatThrownBy(() -> gameDayStartService.startDay(mock(Authentication.class)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
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
        ReflectionTestUtils.setField(location, "locationName", "loc-" + locationId);
        ReflectionTestUtils.setField(location, "interiorCost", 200_000);

        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", menuId);
        ReflectionTestUtils.setField(menu, "originPrice", originPrice);
        ReflectionTestUtils.setField(menu, "menuName", "cookie");

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

    private com.ssafy.S14P21A205.game.season.entity.DailyReport previousDailyReport(Store store, int balance, int stock) {
        com.ssafy.S14P21A205.game.season.entity.DailyReport report =
                instantiate(com.ssafy.S14P21A205.game.season.entity.DailyReport.class);
        ReflectionTestUtils.setField(report, "id", 5L);
        ReflectionTestUtils.setField(report, "store", store);
        ReflectionTestUtils.setField(report, "day", 1);
        ReflectionTestUtils.setField(report, "menuName", store.getMenu().getMenuName());
        ReflectionTestUtils.setField(report, "balance", balance);
        ReflectionTestUtils.setField(report, "stockRemaining", stock);
        return report;
    }

    private NewsReport newsReport(
            Season season,
            int day,
            String areaEntryRanking,
            String trendKeywordRanking
    ) {
        NewsReport newsReport = instantiate(NewsReport.class);
        ReflectionTestUtils.setField(newsReport, "id", 11L);
        ReflectionTestUtils.setField(newsReport, "season", season);
        ReflectionTestUtils.setField(newsReport, "day", day);
        ReflectionTestUtils.setField(newsReport, "areaRevenueRanking", "[]");
        ReflectionTestUtils.setField(newsReport, "areaTrafficRanking", "[]");
        ReflectionTestUtils.setField(newsReport, "menuEntryRanking", "[]");
        ReflectionTestUtils.setField(newsReport, "trendKeywordRanking", trendKeywordRanking);
        ReflectionTestUtils.setField(newsReport, "areaEntryRanking", areaEntryRanking);
        return newsReport;
    }

    private DailyEvent dailyEvent(
            Season season,
            int day,
            String eventType,
            String populationRate,
            int capitalFlat,
            Long targetLocationId,
            Long targetMenuId
    ) {
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
        ReflectionTestUtils.setField(dailyEvent, "targetLocationId", targetLocationId);
        ReflectionTestUtils.setField(dailyEvent, "targetMenuId", targetMenuId);
        return dailyEvent;
    }

    private Population population(Location location, LocalDateTime dateTime, int floatingPopulation) {
        Population population = instantiate(Population.class);
        ReflectionTestUtils.setField(population, "location", location);
        ReflectionTestUtils.setField(population, "date", dateTime);
        ReflectionTestUtils.setField(population, "floatingPopulation", floatingPopulation);
        return population;
    }

    private Traffic traffic(Location location, LocalDateTime dateTime, TrafficStatus trafficStatus) {
        Traffic traffic = instantiate(Traffic.class);
        ReflectionTestUtils.setField(traffic, "location", location);
        ReflectionTestUtils.setField(traffic, "date", dateTime);
        ReflectionTestUtils.setField(traffic, "trafficStatus", trafficStatus);
        return traffic;
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
