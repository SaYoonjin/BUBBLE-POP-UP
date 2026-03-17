package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.model.OpeningState;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentPolicy {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final String BALANCE_KEY_PREFIX = "balance:";
    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private final DailyReportRepository dailyReportRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ItemUserRepository itemUserRepository;
    private final StoreRankingPolicy marketRankingPolicy;

    public OpeningState resolveStartingState(
            Store store,
            int day,
            List<Order> regularOrders,
            GameDayStartResponse.MarketSnapshot marketSnapshot
    ) {
        int carriedBalance;
        int carriedStock;
        String previousMenuName = null;
        String previousLocationName = null;

        if (day == 1) {
            Integer persistedBalance = getPersistedBalance(store.getId());
            carriedBalance = persistedBalance == null ? INITIAL_CAPITAL : persistedBalance;
            carriedStock = 0;
        } else {
            DailyReport previousDay = dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
            carriedBalance = previousDay.getBalance();
            carriedStock = previousDay.getStockRemaining();
            previousMenuName = previousDay.getMenuName();
            previousLocationName = previousDay.getLocationName();
        }

        BigDecimal rentMultiplier = marketRankingPolicy.resolveRentMultiplier(
                marketSnapshot == null ? null : marketSnapshot.locationPopularityRank()
        );
        BigDecimal rentCouponMultiplier = itemUserRepository
                .findPurchasedDiscountRateByUserIdAndCategory(store.getUser().getId(), ItemCategory.RENT)
                .orElse(DECIMAL_ONE);
        BigDecimal ingredientDiscountMultiplier = itemUserRepository
                .findPurchasedDiscountRateByUserIdAndCategory(store.getUser().getId(), ItemCategory.INGREDIENT)
                .orElse(DECIMAL_ONE);
        BigDecimal trendCostMultiplier = marketSnapshot == null || marketSnapshot.trendMultiplier() == null
                ? DECIMAL_ONE
                : marketSnapshot.trendMultiplier();

        int dailyRentApplied = marketRankingPolicy.apply(store.getLocation().getRent(), rentMultiplier, rentCouponMultiplier);
        int interiorCost = resolveInteriorCost(store, day, previousLocationName);
        int appliedUnitCost = marketRankingPolicy.apply(
                store.getMenu().getOriginPrice(),
                trendCostMultiplier,
                ingredientDiscountMultiplier
        );

        int regularOrderQuantity = regularOrders.stream()
                .map(Order::getQuantity)
                .filter(quantity -> quantity != null && quantity > 0)
                .mapToInt(Integer::intValue)
                .sum();
        int regularOrderCost = Math.multiplyExact(appliedUnitCost, regularOrderQuantity);

        boolean menuChanged = previousMenuName != null
                && !previousMenuName.isBlank()
                && store.getMenu() != null
                && store.getMenu().getMenuName() != null
                && !previousMenuName.equals(store.getMenu().getMenuName());
        int disposalQuantity = menuChanged ? carriedStock : 0;
        int disposalLoss = Math.multiplyExact(disposalQuantity, appliedUnitCost);
        int openingAgedStock = menuChanged ? 0 : carriedStock;
        int openingFreshStock = regularOrderQuantity;
        int fixedCostTotal = Math.addExact(
                Math.addExact(dailyRentApplied, interiorCost),
                Math.addExact(regularOrderCost, disposalLoss)
        );
        int initialBalance = carriedBalance - fixedCostTotal;

        if (initialBalance < 0) {
            int maxAffordableOrderCount = Math.max(
                    0,
                    (carriedBalance - dailyRentApplied - interiorCost) / Math.max(1, appliedUnitCost)
            );
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Insufficient balance for today's fixed costs. "
                            + "maxOrderCount=%d, existingOrderCount=%d, balanceBeforeOrder=%d, dailyRent=%d, interiorCost=%d, appliedUnitCost=%d"
                            .formatted(
                                    maxAffordableOrderCount,
                                    regularOrderQuantity,
                                    carriedBalance,
                                    dailyRentApplied,
                                    interiorCost,
                                    appliedUnitCost
                            )
            );
        }

        return new OpeningState(
                initialBalance,
                Math.addExact(openingAgedStock, openingFreshStock),
                new GameDayStartResponse.OpeningSummary(
                        carriedBalance,
                        carriedStock,
                        regularOrderQuantity,
                        regularOrderCost,
                        dailyRentApplied,
                        interiorCost,
                        disposalQuantity,
                        disposalLoss,
                        0,
                        appliedUnitCost,
                        openingFreshStock,
                        openingAgedStock,
                        fixedCostTotal,
                        normalizeScale(rentMultiplier),
                        normalizeScale(rentCouponMultiplier),
                        normalizeScale(ingredientDiscountMultiplier),
                        DECIMAL_ONE.setScale(2, RoundingMode.HALF_UP),
                        DECIMAL_ONE.setScale(2, RoundingMode.HALF_UP),
                        normalizeScale(trendCostMultiplier)
                )
        );
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

    private int resolveInteriorCost(Store store, int day, String previousLocationName) {
        if (store.getLocation() == null || store.getLocation().getInteriorCost() == null) {
            return 0;
        }
        if (day == 1) {
            return store.getLocation().getInteriorCost();
        }
        if (previousLocationName == null || previousLocationName.isBlank()) {
            return 0;
        }
        return previousLocationName.equals(store.getLocation().getLocationName())
                ? 0
                : store.getLocation().getInteriorCost();
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        if (value == null) {
            return DECIMAL_ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
