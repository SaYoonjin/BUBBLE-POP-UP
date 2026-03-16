package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import org.springframework.stereotype.Component;

@Component
public class BankruptcyPolicy {

    public BankruptcyResult resolve(DailyReport previousDayReport, long netProfit) {
        int consecutiveDeficitDays = calculateConsecutiveDeficitDays(previousDayReport, netProfit);
        boolean isBankrupt = consecutiveDeficitDays >= 3;
        return new BankruptcyResult(consecutiveDeficitDays, isBankrupt);
    }

    private int calculateConsecutiveDeficitDays(DailyReport previousDayReport, long netProfit) {
        if (netProfit >= 0L) {
            return 0;
        }
        if (previousDayReport == null || previousDayReport.getConsecutiveDeficitDays() == null) {
            return 1;
        }
        return previousDayReport.getConsecutiveDeficitDays() + 1;
    }

    public record BankruptcyResult(
            int consecutiveDeficitDays,
            boolean bankrupt
    ) {
    }
}
