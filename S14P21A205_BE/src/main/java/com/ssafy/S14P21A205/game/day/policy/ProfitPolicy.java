package com.ssafy.S14P21A205.game.day.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ProfitPolicy {

    private static final BigDecimal ZERO_ROI = new BigDecimal("0.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public ProfitResult calculate(Long cumulativeSales, Long cumulativeTotalCost) {
        long revenue = valueOf(cumulativeSales);
        long totalCost = valueOf(cumulativeTotalCost);
        long netProfit = revenue - totalCost;
        return new ProfitResult(revenue, totalCost, netProfit);
    }

    public BigDecimal calculateRoi(long totalRevenue, long totalCost) {
        if (totalCost <= 0L) {
            return ZERO_ROI;
        }

        return BigDecimal.valueOf(totalRevenue - totalCost)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalCost), 1, RoundingMode.HALF_UP);
    }

    private long valueOf(Long value) {
        return value == null ? 0L : value;
    }

    public record ProfitResult(
            long revenue,
            long totalCost,
            long netProfit
    ) {
    }
}
