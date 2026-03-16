package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.model.OpeningState;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentPolicy {

    private static final int INITIAL_CAPITAL = 10_000_000;
    private static final String BALANCE_KEY_PREFIX = "balance:";

    private final DailyReportRepository dailyReportRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public OpeningState resolveStartingState(Store store, int day, Order existingOrder) {
        int orderCount = existingOrder == null ? 0 : existingOrder.getQuantity();
        int orderCost = existingOrder == null ? 0 : existingOrder.getTotalCost();

        int carriedBalance;
        int carriedStock;
        int orderCostToDeduct = orderCost;
        if (day == 1) {
            Integer persistedBalance = getPersistedBalance(store.getId());
            carriedBalance = persistedBalance == null ? INITIAL_CAPITAL : persistedBalance;
            if (persistedBalance != null) {
                orderCostToDeduct = 0;
            }
            carriedStock = 0;
        } else {
            DailyReport previousDay = dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
            carriedBalance = previousDay.getBalance();
            carriedStock = previousDay.getStockRemaining();
        }

        int dailyRent = store.getLocation().getRent();
        int balanceAfterDailyRent = carriedBalance - dailyRent;
        int initialBalance = balanceAfterDailyRent - orderCostToDeduct;
        if (initialBalance < 0) {
            int maxAffordableOrderCount = Math.max(0, balanceAfterDailyRent / store.getMenu().getOriginPrice());
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Insufficient balance for today's fixed costs. "
                            + "maxOrderCount=%d, existingOrderCount=%d, balanceBeforeOrder=%d, dailyRent=%d, originPrice=%d"
                            .formatted(
                                    maxAffordableOrderCount,
                                    orderCount,
                                    carriedBalance,
                                    dailyRent,
                                    store.getMenu().getOriginPrice()
                            )
            );
        }

        return new OpeningState(initialBalance, Math.addExact(carriedStock, orderCount), orderCount, orderCost);
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
}
