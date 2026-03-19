package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.store.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class CostPolicy {

    public CostResult calculate(
            Store store,
            Order dailyStartOrder,
            GameDayStartResponse startResponse,
            long actionTotalCost,
            long emergencyOrderTotalCost,
            long locationChangeCost,
            long capitalChange,
            long cumulativeSales,
            int initialBalance
    ) {
        // TODO: Apply event-driven cost multipliers here if RandomEvent.costRate becomes part of live cost rules.
        long cumulativeTotalCost = resolveOpeningFixedCost(store, dailyStartOrder, startResponse)
                + actionTotalCost
                + emergencyOrderTotalCost
                + locationChangeCost;
        long cash = initialBalance
                + cumulativeSales
                + capitalChange
                - actionTotalCost
                - emergencyOrderTotalCost
                - locationChangeCost;
        return new CostResult(cumulativeSales, cumulativeTotalCost, cash);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private long resolveOpeningFixedCost(Store store, Order dailyStartOrder, GameDayStartResponse startResponse) {
        if (startResponse != null
                && startResponse.openingSummary() != null
                && startResponse.openingSummary().fixedCostTotal() != null) {
            return valueOf(startResponse.openingSummary().fixedCostTotal());
        }
        return valueOf(store.getLocation() == null ? null : store.getLocation().getRent())
                + valueOf(dailyStartOrder == null ? null : dailyStartOrder.getTotalCost());
    }

    public record CostResult(
            long cumulativeSales,
            long cumulativeTotalCost,
            long cash
    ) {
    }
}
