package com.ssafy.S14P21A205.action.config;

import com.ssafy.S14P21A205.action.entity.Action;
import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import com.ssafy.S14P21A205.action.repository.ActionRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ActionDataInitializer {

    private final ActionRepository actionRepository;

    @Bean
    ApplicationRunner initializeActions() {
        return args -> {
            seedSingle(ActionCategory.DISCOUNT, null, 500, "0.00");
            seedSingle(ActionCategory.DONATION, null, 0, "0.00");
            seedSingle(ActionCategory.EMERGENCY_ORDER, null, 500, "0.00");
            seedSingle(ActionCategory.PROMOTION, PromotionType.INFLUENCER, 50_000, "0.20");
            seedSingle(ActionCategory.PROMOTION, PromotionType.SNS, 30_000, "0.15");
            seedSingle(ActionCategory.PROMOTION, PromotionType.LEAFLET, 10_000, "0.10");
            seedSingle(ActionCategory.PROMOTION, PromotionType.FRIEND, 0, "0.05");
        };
    }

    private void seedSingle(
            ActionCategory category,
            PromotionType promotionType,
            int cost,
            String captureRate
    ) {
        boolean exists;
        if (promotionType == null) {
            exists = actionRepository.findByCategory(category) != null
                    && !actionRepository.findByCategory(category).isEmpty();
        } else {
            exists = actionRepository.findByCategoryAndPromotionType(category, promotionType) != null
                    && actionRepository.findByCategoryAndPromotionType(category, promotionType).isPresent();
        }
        if (exists) {
            return;
        }
        actionRepository.save(Action.create(
                category,
                promotionType,
                cost,
                new BigDecimal(captureRate)
        ));
    }
}
