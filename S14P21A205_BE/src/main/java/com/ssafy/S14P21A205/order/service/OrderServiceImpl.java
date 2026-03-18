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
import java.math.RoundingMode;
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
    private static final BigDecimal RECOMMENDED_PRICE_MULTIPLIER = new BigDecimal("2.5");

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
        PricingPolicy pricingPolicy = resolvePricingPolicy(menu, ingredientDiscountRate, menuTrendRank);
        Integer stock = resolveStock(storeId, currentDay);

        return CurrentOrderResponse.builder()
                .menuId(Math.toIntExact(menu.getId()))
                .menuName(menu.getMenuName())
                .costPrice(pricingPolicy.costPrice())
                .recommendedPrice(pricingPolicy.recommendedPrice())
                .maxSellingPrice(pricingPolicy.maxSellingPrice())
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
        List<Store> seasonStores = storeRepository.findBySeason_IdOrderByIdAsc(store.getSeason().getId());

        BigDecimal discountRate = getIngredientDiscountRate(store.getUser().getId());
        int menuTrendRank = marketRankingPolicy.resolveMenuTrendRank(menu.getId(), seasonStores);
        PricingPolicy pricingPolicy = resolvePricingPolicy(menu, discountRate, menuTrendRank);
        Integer sellingPrice = resolveSellingPrice(request.price(), sameMenu, store.getPrice(), pricingPolicy);
        validateSellingPrice(sellingPrice, pricingPolicy);
        Integer totalCost = Math.multiplyExact(pricingPolicy.costPrice(), request.quantity());

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
                .costPrice(pricingPolicy.costPrice())
                .sellingPrice(sellingPrice)
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
                .orElseThrow(() -> new BaseException(ErrorCode.MENU_NOT_FOUND));
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

    private Integer resolveMinimumSellingPrice(Menu menu, int menuTrendRank) {
        return marketRankingPolicy.apply(
                menu.getOriginPrice(),
                marketRankingPolicy.resolveTrendMultiplier(menuTrendRank)
        );
    }

    private PricingPolicy resolvePricingPolicy(Menu menu, BigDecimal discountRate, int menuTrendRank) {
        int costPrice = resolveCostPrice(menu, discountRate, menuTrendRank);
        int minimumSellingPrice = resolveMinimumSellingPrice(menu, menuTrendRank);
        int recommendedPrice = BigDecimal.valueOf(minimumSellingPrice)
                .multiply(RECOMMENDED_PRICE_MULTIPLIER)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int maxSellingPrice = Math.multiplyExact(recommendedPrice, 2);
        return new PricingPolicy(costPrice, minimumSellingPrice, recommendedPrice, maxSellingPrice);
    }

    private Integer resolveSellingPrice(
            Integer requestedPrice,
            boolean sameMenu,
            Integer currentStorePrice,
            PricingPolicy pricingPolicy
    ) {
        if (requestedPrice != null) {
            return requestedPrice;
        }
        if (sameMenu && currentStorePrice != null && isSellingPriceWithinRange(currentStorePrice, pricingPolicy)) {
            return currentStorePrice;
        }
        return pricingPolicy.recommendedPrice();
    }

    private void validateSellingPrice(Integer sellingPrice, PricingPolicy pricingPolicy) {
        if (sellingPrice == null || !isSellingPriceWithinRange(sellingPrice, pricingPolicy)) {
            throw new BaseException(
                    ErrorCode.ORDER_INVALID_SELLING_PRICE,
                    "Selling price must be between %d and %d."
                            .formatted(pricingPolicy.minimumSellingPrice(), pricingPolicy.maxSellingPrice())
            );
        }
    }

    private boolean isSellingPriceWithinRange(int sellingPrice, PricingPolicy pricingPolicy) {
        return sellingPrice >= pricingPolicy.minimumSellingPrice()
                && sellingPrice <= pricingPolicy.maxSellingPrice();
    }

    private void validateRegularOrderDay(int currentDay) {
        if (!REGULAR_ORDER_DAYS.contains(currentDay)) {
            throw new BaseException(
                    ErrorCode.ORDER_NOT_AVAILABLE_DAY,
                    "Regular orders are only available on days 1, 3, 5, and 7."
            );
        }
    }

    private int resolveRegularOrderDay(Integer currentDay) {
        return currentDay == null || currentDay == 0 ? 1 : currentDay;
    }

    private void validateDayNotStarted(Long storeId, int day) {
        if (gameDayStoreStateRedisRepository.exists(storeId, day)) {
            throw new BaseException(ErrorCode.ORDER_DAY_ALREADY_STARTED);
        }
    }

    private void validateNoExistingOrder(Long storeId, Integer orderedDay) {
        if (orderRepository.findDailyStartOrder(storeId, orderedDay).isPresent()) {
            throw new BaseException(ErrorCode.ORDER_ALREADY_EXISTS);
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 50 || quantity > 500) {
            throw new BaseException(ErrorCode.ORDER_INVALID_QUANTITY);
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
            throw new BaseException(ErrorCode.ORDER_INSUFFICIENT_BALANCE);
        }
    }

    private int resolveCarriedBalance(Store store, int day) {
        if (day == 1) {
            Integer persistedBalance = getPersistedBalance(store.getId());
            return persistedBalance == null ? INITIAL_CAPITAL : persistedBalance;
        }

        return dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .map(report -> report.getBalance() == null ? 0 : report.getBalance())
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND, "Previous day report not found."));
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
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND, "Previous day report not found."));
    }

    private record PricingPolicy(
            int costPrice,
            int minimumSellingPrice,
            int recommendedPrice,
            int maxSellingPrice
    ) {
    }
}
