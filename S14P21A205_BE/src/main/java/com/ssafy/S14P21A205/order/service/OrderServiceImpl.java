package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.policy.StoreRankingPolicy;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.dto.CurrentOrderResponse;
import com.ssafy.S14P21A205.order.dto.RegularOrderRequest;
import com.ssafy.S14P21A205.order.dto.RegularOrderResponse;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(1, 3, 5, 7);
    private static final String BALANCE_KEY_PREFIX = "balance:";

    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final ItemUserRepository itemUserRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final StoreRankingPolicy marketRankingPolicy;

    @Override
    public CurrentOrderResponse getCurrentOrder(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        int currentDay = resolveRegularOrderDay(store.getSeason().getCurrentDay());
        Menu menu = store.getMenu();
        List<Store> seasonStores = storeRepository.findBySeason_IdOrderByIdAsc(store.getSeason().getId());

        BigDecimal ingredientDiscountRate = getIngredientDiscountRate(store.getUser().getId());
        int menuTrendRank = marketRankingPolicy.resolveMenuTrendRank(menu.getId(), seasonStores);
        Integer discountedCostPrice = resolveCostPrice(menu, ingredientDiscountRate, menuTrendRank);
        Integer stock = resolveStock(storeId, currentDay);

        return CurrentOrderResponse.builder()
                .menuId(Math.toIntExact(menu.getId()))
                .menuName(menu.getMenuName())
                .costPrice(discountedCostPrice)
                .sellingPrice(store.getPrice())
                .stock(stock)
                .build();
    }

    @Override
    @Transactional
    public RegularOrderResponse createRegularOrder(Integer userId, RegularOrderRequest request) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        int regularOrderDay = resolveRegularOrderDay(store.getSeason().getCurrentDay());

        validateRegularOrderDay(regularOrderDay);
        validateQuantity(request.quantity());
        validateDayNotStarted(storeId, regularOrderDay);
        validateNoExistingOrder(storeId, regularOrderDay);

        Menu menu = getMenuById(request.menuId());
        boolean sameMenu = Objects.equals(store.getMenu().getId(), menu.getId());
        Integer sellingPrice = resolveSellingPrice(request.price(), store.getPrice(), menu.getOriginPrice());
        List<Store> seasonStores = storeRepository.findBySeason_IdOrderByIdAsc(store.getSeason().getId());

        BigDecimal discountRate = getIngredientDiscountRate(store.getUser().getId());
        int menuTrendRank = marketRankingPolicy.resolveMenuTrendRank(menu.getId(), seasonStores);
        Integer costPrice = resolveCostPrice(menu, discountRate, menuTrendRank);
        Integer totalCost = Math.multiplyExact(costPrice, request.quantity());

        validateAffordableOrder(store, regularOrderDay, totalCost, seasonStores);

        if (!Objects.equals(store.getPrice(), sellingPrice)) {
            store.changePrice(sellingPrice);
        }

        if (!sameMenu) {
            store.changeMenu(menu);
        }

        Order savedOrder = orderRepository.save(
                Order.create(menu, store, request.quantity(), totalCost, regularOrderDay)
        );

        return RegularOrderResponse.builder()
                .orderId(savedOrder.getId())
                .menuId(Math.toIntExact(menu.getId()))
                .quantity(request.quantity())
                .costPrice(costPrice)
                .totalCost(totalCost)
                .discount(discountRate.floatValue())
                .build();
    }

    private Store getStoreByUserId(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
    }

    private Menu getMenuById(Integer menuId) {
        return menuRepository.findById(Long.valueOf(menuId))
                .orElseThrow(() -> new RuntimeException("Menu was not found."));
    }

    private BigDecimal getIngredientDiscountRate(Integer userId) {
        return itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(userId, ItemCategory.INGREDIENT)
                .orElse(BigDecimal.ONE);
    }

    private BigDecimal getRentDiscountRate(Integer userId) {
        return itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(userId, ItemCategory.RENT)
                .orElse(BigDecimal.ONE);
    }

    private Integer resolveCostPrice(Menu menu, BigDecimal discountRate, int menuTrendRank) {
        return marketRankingPolicy.apply(
                menu.getOriginPrice(),
                marketRankingPolicy.resolveTrendMultiplier(menuTrendRank),
                discountRate
        );
    }

    private Integer resolveSellingPrice(Integer requestedPrice, Integer currentStorePrice, Integer defaultPrice) {
        if (requestedPrice != null) {
            return requestedPrice;
        }
        if (currentStorePrice != null) {
            return currentStorePrice;
        }
        return defaultPrice;
    }

    private void validateRegularOrderDay(int currentDay) {
        if (!REGULAR_ORDER_DAYS.contains(currentDay)) {
            throw new RuntimeException("Regular orders are only available on days 1, 3, 5, and 7.");
        }
    }

    private int resolveRegularOrderDay(Integer currentDay) {
        return currentDay == null || currentDay == 0 ? 1 : currentDay;
    }

    private void validateDayNotStarted(Long storeId, int day) {
        if (gameDayStoreStateRedisRepository.exists(storeId, day)) {
            throw new RuntimeException("Regular orders are unavailable after the day has started.");
        }
    }

    private void validateNoExistingOrder(Long storeId, Integer orderedDay) {
        if (orderRepository.findDailyStartOrder(storeId, orderedDay).isPresent()) {
            throw new RuntimeException("A regular order already exists for this day.");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 50 || quantity > 500) {
            throw new RuntimeException("Order quantity must be between 50 and 500.");
        }
    }

    private void validateAffordableOrder(Store store, int day, int totalCost, List<Store> seasonStores) {
        int carriedBalance = resolveCarriedBalance(store, day);
        int locationRank = marketRankingPolicy.resolveLocationPopularityRank(store.getLocation().getId(), seasonStores);
        int dailyRentApplied = marketRankingPolicy.apply(
                store.getLocation().getRent(),
                marketRankingPolicy.resolveRentMultiplier(locationRank),
                getRentDiscountRate(store.getUser().getId())
        );
        int interiorCost = resolveInteriorCost(store, day);

        if (carriedBalance - dailyRentApplied - interiorCost < totalCost) {
            throw new RuntimeException("Insufficient balance for this regular order.");
        }
    }

    private int resolveCarriedBalance(Store store, int day) {
        if (day == 1) {
            Integer persistedBalance = getPersistedBalance(store.getId());
            return persistedBalance == null ? INITIAL_CAPITAL : persistedBalance;
        }

        return dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .map(report -> report.getBalance() == null ? 0 : report.getBalance())
                .orElseThrow(() -> new RuntimeException("Previous day report not found."));
    }

    private Integer getPersistedBalance(Long storeId) {
        String value = stringRedisTemplate.opsForValue().get(balanceKey(storeId));
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private String balanceKey(Long storeId) {
        return BALANCE_KEY_PREFIX + storeId;
    }

    private Integer resolveStock(Long storeId, int day) {
        return gameDayStoreStateRedisRepository.find(storeId, day)
                .map(state -> state.stock() == null ? 0 : state.stock())
                .orElseGet(() -> resolveStartingStock(storeId, day));
    }

    private Integer resolveStartingStock(Long storeId, int day) {
        int carriedStock = day == 1
                ? 0
                : dailyReportRepository.findByStoreIdAndDay(storeId, day - 1)
                .map(report -> report.getStockRemaining() == null ? 0 : report.getStockRemaining())
                .orElse(0);

        int orderedStock = orderRepository.findDailyStartOrder(storeId, day)
                .map(Order::getQuantity)
                .orElse(0);

        return Math.addExact(carriedStock, orderedStock);
    }

    private int resolveInteriorCost(Store store, int day) {
        if (store.getLocation() == null || store.getLocation().getInteriorCost() == null) {
            return 0;
        }
        if (day == 1) {
            return store.getLocation().getInteriorCost();
        }

        return dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .map(report -> {
                    String previousLocationName = report.getLocationName();
                    if (previousLocationName == null || previousLocationName.isBlank()) {
                        return 0;
                    }
                    return previousLocationName.equals(store.getLocation().getLocationName())
                            ? 0
                            : store.getLocation().getInteriorCost();
                })
                .orElseThrow(() -> new RuntimeException("Previous day report not found."));
    }
}
