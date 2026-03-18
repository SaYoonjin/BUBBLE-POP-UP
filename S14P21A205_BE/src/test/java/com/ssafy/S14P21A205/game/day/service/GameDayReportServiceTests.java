package com.ssafy.S14P21A205.game.day.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.policy.BankruptcyPolicy;
import com.ssafy.S14P21A205.game.day.policy.ProfitPolicy;
import com.ssafy.S14P21A205.game.day.policy.ReputationPolicy;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
class GameDayReportServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    @Mock
    private WeatherRepository weatherRepository;

    private GameDayReportService gameDayReportService;

    @BeforeEach
    void setUp() {
        ProfitPolicy profitPolicy = new ProfitPolicy();
        ReputationPolicy reputationPolicy = new ReputationPolicy();
        BankruptcyPolicy bankruptcyPolicy = new BankruptcyPolicy();
        gameDayReportService = new GameDayReportService(
                userService,
                storeRepository,
                dailyReportRepository,
                gameDayStoreStateRedisRepository,
                weatherRepository,
                profitPolicy,
                reputationPolicy,
                bankruptcyPolicy
        );
        ReflectionTestUtils.setField(
                gameDayReportService,
                "clock",
                Clock.fixed(Instant.parse("2026-03-09T05:33:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void recordClosedDayReportSavesReportDuringReportPhase() {
        Store store = store(15L, 9L, 2, 7, "Seongsu", "Cookie", 300);
        GameDayLiveState state = state(
                LocalDateTime.of(2026, 3, 9, 14, 30, 0),
                new BigDecimal("0.10"),
                5_000L,
                1_300L,
                42,
                20,
                15_000L,
                12
        );

        when(dailyReportRepository.existsByStoreIdAndDay(15L, 2)).thenReturn(false);
        when(gameDayStoreStateRedisRepository.find(15L, 2)).thenReturn(Optional.of(state));
        when(dailyReportRepository.findByStoreIdAndDay(15L, 1)).thenReturn(Optional.empty());

        gameDayReportService.recordClosedDayReport(store);

        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        verify(dailyReportRepository).save(captor.capture());

        DailyReport saved = captor.getValue();
        assertThat(saved.getDay()).isEqualTo(2);
        assertThat(saved.getLocationName()).isEqualTo("Seongsu");
        assertThat(saved.getMenuName()).isEqualTo("Cookie");
        assertThat(saved.getRevenue()).isEqualTo(5_000);
        assertThat(saved.getTotalCost()).isEqualTo(1_300);
        assertThat(saved.getNetProfit()).isEqualTo(3_700);
        assertThat(saved.getVisitors()).isEqualTo(42);
        assertThat(saved.getSalesCount()).isEqualTo(20);
        assertThat(saved.getStockRemaining()).isEqualTo(12);
        assertThat(saved.getConsecutiveDeficitDays()).isZero();
        assertThat(saved.getIsBankrupt()).isFalse();
        assertThat(saved.getBalance()).isEqualTo(15_000);
        assertThat(saved.getCaptureRate()).isEqualByComparingTo("0.10");
    }

    @Test
    void getDayReportReturnsComputedFields() {
        User user = user(1);
        Store store = store(15L, 9L, 2, 7, "Current Location", "Current Menu", 300);
        DailyReport dayOne = dailyReport(
                store,
                1,
                "Old Location",
                "Old Menu",
                1_000,
                800,
                200,
                10,
                8,
                4,
                0,
                false,
                10_200,
                new BigDecimal("0.04")
        );
        DailyReport dayTwo = dailyReport(
                store,
                2,
                "Seongsu",
                "Cookie",
                5_000,
                1_300,
                3_700,
                42,
                20,
                12,
                2,
                false,
                15_000,
                new BigDecimal("0.10")
        );

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(dailyReportRepository.findByStoreIdAndDay(15L, 2)).thenReturn(Optional.of(dayTwo));
        when(dailyReportRepository.findByStoreIdAndDay(15L, 1)).thenReturn(Optional.of(dayOne));
        when(weatherRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                weather(WeatherType.SUNNY),
                weather(WeatherType.RAIN),
                weather(WeatherType.SNOW)
        ));

        GameDayReportResponse response = gameDayReportService.getDayReport(mock(Authentication.class), 2);

        assertThat(response.seasonId()).isEqualTo(9L);
        assertThat(response.day()).isEqualTo(2);
        assertThat(response.locationName()).isEqualTo("Seongsu");
        assertThat(response.menuName()).isEqualTo("Cookie");
        assertThat(response.revenue()).isEqualTo(5_000L);
        assertThat(response.totalCost()).isEqualTo(1_300L);
        assertThat(response.netProfit()).isEqualTo(3_700L);
        assertThat(response.visitors()).isEqualTo(42);
        assertThat(response.salesCount()).isEqualTo(20);
        assertThat(response.stockRemaining()).isEqualTo(12);
        assertThat(response.stockDisposedCount()).isZero();
        assertThat(response.reputationScore()).isEqualByComparingTo("0.5");
        assertThat(response.reputationChange()).isEqualByComparingTo("0.3");
        assertThat(response.tomorrowWeather()).isNotNull();
        assertThat(response.tomorrowWeather().condition()).isEqualTo("SNOW");
        assertThat(response.isNextDayOrderDay()).isTrue();
        assertThat(response.consecutiveDeficitDays()).isEqualTo(2);
        assertThat(response.isBankrupt()).isFalse();
    }

    @Test
    void getDayReportThrowsWhenDayIsOutOfRange() {
        User user = user(1);
        Store store = store(15L, 9L, 2, 7, "Seongsu", "Cookie", 300);

        when(userService.getCurrentUser(any())).thenReturn(user);
        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));

        assertThatThrownBy(() -> gameDayReportService.getDayReport(mock(Authentication.class), 8))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getErrorCode()).isEqualTo(ErrorCode.INVALID_DAY);
                });
    }

    private User user(int id) {
        User user = new User("test@example.com", "tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Store store(
            Long storeId,
            Long seasonId,
            int currentDay,
            int totalDays,
            String locationName,
            String menuName,
            int rent
    ) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", 3L);
        ReflectionTestUtils.setField(location, "locationName", locationName);
        ReflectionTestUtils.setField(location, "rent", rent);

        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", 5L);
        ReflectionTestUtils.setField(menu, "menuName", menuName);
        ReflectionTestUtils.setField(menu, "originPrice", 500);

                Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", seasonId);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(season, "currentDay", currentDay);
        ReflectionTestUtils.setField(season, "totalDays", totalDays);
        LocalDateTime reportPhaseAt = LocalDateTime.of(2026, 3, 9, 14, 33, 0);
        LocalDateTime seasonStartAt = reportPhaseAt.minusSeconds(120L + (currentDay - 1L) * 180L + 170L);
        ReflectionTestUtils.setField(season, "startTime", seasonStartAt);
        ReflectionTestUtils.setField(season, "endTime", seasonStartAt.plusSeconds(120L + totalDays * 180L + 120L));

        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "menu", menu);
        ReflectionTestUtils.setField(store, "season", season);
        ReflectionTestUtils.setField(store, "storeName", "Ignored Store Name");
        return store;
    }

    private GameDayLiveState state(
            LocalDateTime startedAt,
            BigDecimal captureRate,
            Long cumulativeSales,
            Long cumulativeTotalCost,
            Integer cumulativeCustomerCount,
            Integer cumulativePurchaseCount,
            Long balance,
            Integer stock
    ) {
        return new GameDayLiveState(
                startedAt,
                List.of(),
                0,
                null,
                18,
                0,
                captureRate,
                500,
                0,
                0,
                0L,
                cumulativeCustomerCount,
                cumulativePurchaseCount,
                cumulativeSales,
                cumulativeTotalCost,
                balance,
                stock,
                LocalDateTime.of(2026, 3, 9, 14, 33, 0)
        );
    }

    private DailyReport dailyReport(
            Store store,
            int day,
            String locationName,
            String menuName,
            int revenue,
            int totalCost,
            int netProfit,
            int visitors,
            int salesCount,
            int stockRemaining,
            int consecutiveDeficitDays,
            boolean isBankrupt,
            int balance,
            BigDecimal captureRate
    ) {
        return DailyReport.create(
                store,
                day,
                locationName,
                menuName,
                revenue,
                totalCost,
                netProfit,
                visitors,
                salesCount,
                stockRemaining,
                consecutiveDeficitDays,
                isBankrupt,
                balance,
                captureRate
        );
    }

    private Weather weather(WeatherType weatherType) {
        Weather weather = instantiate(Weather.class);
        ReflectionTestUtils.setField(weather, "id", 1L);
        ReflectionTestUtils.setField(weather, "weatherType", weatherType);
        ReflectionTestUtils.setField(weather, "populationPercent", BigDecimal.ONE);
        return weather;
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


