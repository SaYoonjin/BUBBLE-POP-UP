package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
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
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(1, 2, 4, 6);

    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final ItemUserRepository itemUserRepository;

    @Override
    public CurrentOrderResponse getCurrentOrder(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        int currentDay = resolveRegularOrderDay(store.getSeason().getCurrentDay());
        Menu menu = store.getMenu();

        BigDecimal ingredientDiscountRate = getIngredientDiscountRate(store.getUser().getId());
        Integer discountedCostPrice = applyDiscount(menu.getOriginPrice(), ingredientDiscountRate);
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

        BigDecimal discountRate = getIngredientDiscountRate(store.getUser().getId());
        Integer costPrice = resolveCostPrice(menu, discountRate);
        Integer totalCost = Math.multiplyExact(costPrice, request.quantity());

        validateAffordableOrder(store, regularOrderDay, totalCost);

        if (!Objects.equals(store.getPrice(), sellingPrice)) {
            store.changePrice(sellingPrice);
        }

        Order savedOrder = orderRepository.save(
                Order.create(menu, store, request.quantity(), totalCost, regularOrderDay)
        );

        if (!sameMenu) {
            store.changeMenu(menu);
        }

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

    private Integer resolveCostPrice(Menu menu, BigDecimal discountRate) {
        return applyDiscount(menu.getOriginPrice(), discountRate);
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

    private Integer applyDiscount(Integer originalPrice, BigDecimal discountRate) {
        if (discountRate == null || discountRate.signum() <= 0) {
            return originalPrice;
        }

        return BigDecimal.valueOf(originalPrice)
                .multiply(discountRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private void validateRegularOrderDay(int currentDay) {
        if (!REGULAR_ORDER_DAYS.contains(currentDay)) {
            throw new RuntimeException("Regular orders are only available on days 1, 2, 4, and 6.");
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

    private void validateAffordableOrder(Store store, int day, int totalCost) {
        int carriedBalance = day == 1
                ? INITIAL_CAPITAL
                : dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .map(report -> report.getBalance() == null ? 0 : report.getBalance())
                .orElseThrow(() -> new RuntimeException("Previous day report not found."));

        int balanceAfterDailyRent = carriedBalance - store.getLocation().getRent();
        if (balanceAfterDailyRent < totalCost) {
            throw new RuntimeException("Insufficient balance for this regular order.");
        }
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
}