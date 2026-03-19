package com.ssafy.S14P21A205.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.policy.StoreRankingPolicy;
import com.ssafy.S14P21A205.game.day.resolver.NewsRankingResolver;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.dto.RegularOrderRequest;
import com.ssafy.S14P21A205.order.dto.RegularOrderResponse;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    @Mock
    private ItemUserRepository itemUserRepository;

    @Mock
    private NewsRankingResolver newsRankingResolver;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                storeRepository,
                menuRepository,
                orderRepository,
                dailyReportRepository,
                gameDayStoreStateRedisRepository,
                itemUserRepository,
                new StoreRankingPolicy(),
                newsRankingResolver,
                Clock.fixed(Instant.parse("2026-03-17T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void createRegularOrderPersistsSellingPriceToOrderSalePrice() {
        Store store = store(15L, 1, 3L, 7L, 2_500, 4_000);
        Menu menu = store.getMenu();

        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.exists(15L, 1)).thenReturn(false);
        when(orderRepository.findDailyStartOrder(15L, 1)).thenReturn(Optional.empty());
        when(menuRepository.findById(7L)).thenReturn(Optional.of(menu));
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(1, ItemCategory.INGREDIENT))
                .thenReturn(Optional.of(BigDecimal.ONE));
        when(itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(1, ItemCategory.RENT))
                .thenReturn(Optional.of(BigDecimal.ONE));
        when(newsRankingResolver.resolveMenuEntryRank(9L, 1, menu)).thenReturn(null);
        when(newsRankingResolver.resolveAreaEntryRank(9L, 1, store.getLocation())).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        RegularOrderResponse response = orderService.createRegularOrder(1, new RegularOrderRequest(7, 50, 7_000));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getSalePrice()).isEqualTo(7_000);
        assertThat(orderCaptor.getValue().getTotalCost()).isEqualTo(150_000);
        assertThat(response.orderId()).isEqualTo(101L);
        assertThat(response.sellingPrice()).isEqualTo(7_000);
        assertThat(response.totalCost()).isEqualTo(150_000);
    }

    @Test
    void createRegularOrderThrowsWhenSellingPriceIsBelowAllowedRange() {
        Store store = store(15L, 1, 3L, 7L, 2_500, 4_000);
        Menu menu = store.getMenu();

        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(gameDayStoreStateRedisRepository.exists(15L, 1)).thenReturn(false);
        when(orderRepository.findDailyStartOrder(15L, 1)).thenReturn(Optional.empty());
        when(menuRepository.findById(7L)).thenReturn(Optional.of(menu));
        when(storeRepository.findBySeason_IdOrderByIdAsc(9L)).thenReturn(List.of(store));
        when(itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(1, ItemCategory.INGREDIENT))
                .thenReturn(Optional.of(BigDecimal.ONE));
        when(newsRankingResolver.resolveMenuEntryRank(9L, 1, menu)).thenReturn(null);

        assertThatThrownBy(() -> orderService.createRegularOrder(1, new RegularOrderRequest(7, 50, 2_000)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_INVALID_SELLING_PRICE));
    }

    private Store store(Long storeId, Integer userId, Long locationId, Long menuId, int originPrice, int currentPrice) {
        User user = new User("order@test.com", "tester");
        ReflectionTestUtils.setField(user, "id", userId);

        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", locationId);
        ReflectionTestUtils.setField(location, "locationName", "fixture-location");
        ReflectionTestUtils.setField(location, "rent", 100_000);
        ReflectionTestUtils.setField(location, "interiorCost", 50_000);

        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", menuId);
        ReflectionTestUtils.setField(menu, "menuName", "fixture-menu");
        ReflectionTestUtils.setField(menu, "originPrice", originPrice);

        Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", 9L);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(season, "currentDay", 1);
        ReflectionTestUtils.setField(season, "totalDays", 7);
        ReflectionTestUtils.setField(season, "startTime", LocalDateTime.of(2026, 3, 17, 9, 57, 50));
        ReflectionTestUtils.setField(season, "endTime", LocalDateTime.of(2026, 3, 17, 10, 30, 0));

        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "user", user);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "menu", menu);
        ReflectionTestUtils.setField(store, "season", season);
        ReflectionTestUtils.setField(store, "storeName", "fixture-store");
        ReflectionTestUtils.setField(store, "price", currentPrice);
        return store;
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
