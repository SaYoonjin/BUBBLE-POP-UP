package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String BALANCE_KEY_PREFIX = "balance:";
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(0, 2, 4, 6);

    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final ItemUserRepository itemUserRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CurrentOrderResponse getCurrentOrder(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        Menu menu = store.getMenu();

        BigDecimal ingredientDiscountRate = getIngredientDiscountRate(store.getUser().getId());
        Integer discountedCostPrice = applyDiscount(menu.getOriginPrice(), ingredientDiscountRate);
        Integer stock = getStock(storeId);

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
        int orderedDay = resolveOrderedDay(regularOrderDay);

        validateRegularOrderDay(regularOrderDay);
        validateQuantity(request.quantity());
        validateNoExistingOrder(storeId, orderedDay);

        Menu menu = getMenuById(request.menuId());
        boolean sameMenu = Objects.equals(store.getMenu().getId(), menu.getId());
        Integer sellingPrice = resolveSellingPrice(request.price(), store.getPrice(), menu.getOriginPrice());

        BigDecimal discountRate = getIngredientDiscountRate(store.getUser().getId());
        Integer costPrice = resolveCostPrice(menu, discountRate);
        Integer totalCost = Math.multiplyExact(costPrice, request.quantity());

        if (!Objects.equals(store.getPrice(), sellingPrice)) {
            store.changePrice(sellingPrice);
        }

        deductBalance(storeId, totalCost);
        updateStock(storeId, request.quantity(), sameMenu);
        if (!sameMenu) {
            store.changeMenu(menu);
        }

        Order savedOrder = orderRepository.save(
                Order.create(menu, store, request.quantity(), totalCost, orderedDay)
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
            throw new RuntimeException("Regular orders are only available on days 0, 2, 4, and 6.");
        }
    }

    private int resolveRegularOrderDay(Integer currentDay) {
        return currentDay == null ? 0 : currentDay;
    }

    private int resolveOrderedDay(int regularOrderDay) {
        return regularOrderDay == 0 ? 1 : regularOrderDay;
    }

    private void validateNoExistingOrder(Long storeId, Integer orderedDay) {
        if (orderRepository.findDailyStartOrder(storeId, orderedDay).isPresent()) {
            throw new RuntimeException("A regular order for this day has already been created.");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 50 || quantity > 500) {
            throw new RuntimeException("Order quantity must be between 50 and 500.");
        }
    }

    private Integer getStock(Long storeId) {
        String key = generateStockKey(storeId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        return Integer.valueOf(value);
    }

    private void updateStock(Long storeId, Integer quantity, boolean sameMenu) {
        String key = generateStockKey(storeId);
        int updatedStock = sameMenu ? getStock(storeId) + quantity : quantity;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(updatedStock));
    }

    private void deductBalance(Long storeId, Integer amount) {
        String key = generateBalanceKey(storeId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new RuntimeException("Balance information was not found.");
        }

        Integer currentBalance = Integer.valueOf(value);

        if (currentBalance < amount) {
            throw new RuntimeException("Balance is insufficient.");
        }

        Integer updatedBalance = currentBalance - amount;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(updatedBalance));
    }

    private String generateStockKey(Long storeId) {
        return STOCK_KEY_PREFIX + storeId;
    }

    private String generateBalanceKey(Long storeId) {
        return BALANCE_KEY_PREFIX + storeId;
    }
}
