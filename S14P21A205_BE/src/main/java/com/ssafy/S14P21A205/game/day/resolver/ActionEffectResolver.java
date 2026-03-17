package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import java.math.BigDecimal;
import java.util.List;

public class ActionEffectResolver {

    private static final BigDecimal DECIMAL_ZERO = new BigDecimal("0.00");

    public ActionEffect resolve(List<ActionLog> actionLogs) {
        boolean discountUsed = false;
        boolean donationUsed = false;
        boolean influencerUsed = false;
        boolean snsUsed = false;
        boolean leafletUsed = false;
        boolean friendUsed = false;
        long totalCost = 0L;
        BigDecimal captureRateBoost = DECIMAL_ZERO;

        for (ActionLog actionLog : actionLogs) {
            if (actionLog.getAction() == null) {
                continue;
            }

            totalCost += actionLog.getAction().getCost() == null ? 0L : actionLog.getAction().getCost();

            ActionCategory category = actionLog.getAction().getCategory();
            if (category == ActionCategory.DISCOUNT || category == ActionCategory.DONATION) {
                BigDecimal dynamicValue = actionLog.getActionValue() == null ? DECIMAL_ZERO : actionLog.getActionValue();
                captureRateBoost = captureRateBoost.add(dynamicValue);
            } else {
                captureRateBoost = captureRateBoost.add(
                        actionLog.getAction().getCaptureRate() == null ? DECIMAL_ZERO : actionLog.getAction().getCaptureRate()
                );
            }

            if (category == ActionCategory.DISCOUNT) {
                discountUsed = true;
                continue;
            }
            if (category == ActionCategory.DONATION) {
                donationUsed = true;
                continue;
            }
            if (category != ActionCategory.PROMOTION) {
                continue;
            }

            PromotionType promotionType = actionLog.getAction().getPromotionType();
            if (promotionType == PromotionType.INFLUENCER) {
                influencerUsed = true;
            } else if (promotionType == PromotionType.SNS) {
                snsUsed = true;
            } else if (promotionType == PromotionType.LEAFLET) {
                leafletUsed = true;
            } else if (promotionType == PromotionType.FRIEND) {
                friendUsed = true;
            }
        }

        return new ActionEffect(
                discountUsed,
                donationUsed,
                influencerUsed,
                snsUsed,
                leafletUsed,
                friendUsed,
                totalCost,
                captureRateBoost
        );
    }

    public record ActionEffect(
            boolean discountUsed,
            boolean donationUsed,
            boolean influencerUsed,
            boolean snsUsed,
            boolean leafletUsed,
            boolean friendUsed,
            long totalCost,
            BigDecimal captureRateBoost
    ) {
    }
}
