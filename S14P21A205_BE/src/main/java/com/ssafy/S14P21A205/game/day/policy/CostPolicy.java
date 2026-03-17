package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.store.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class CostPolicy {

    public CostResult calculate(
            Store store,
            Order dailyStartOrder,
            long actionTotalCost,
            long emergencyOrderTotalCost,
            long capitalChange,
            long cumulativeSales,
            int initialBalance
    ) {
        // TODO: Apply event-driven cost multipliers here if RandomEvent.costRate becomes part of live cost rules.
        long cumulativeTotalCost = valueOf(store.getLocation() == null ? null : store.getLocation().getRent())
                + valueOf(dailyStartOrder == null ? null : dailyStartOrder.getTotalCost())
                + actionTotalCost
                + emergencyOrderTotalCost;
        long cash = initialBalance
                + cumulativeSales
                + capitalChange
                - actionTotalCost
                - emergencyOrderTotalCost;
        return new CostResult(cumulativeSales, cumulativeTotalCost, cash);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    public record CostResult(
            long cumulativeSales,
            long cumulativeTotalCost,
            long cash
    ) {
    }
}
