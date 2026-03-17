package com.ssafy.S14P21A205.game.day.generator;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class RandomPurchaseListGenerator implements PurchaseListGenerator {

    private static final int[] PURCHASE_QUANTITY_WEIGHTS = {10, 40, 35, 15};

    @Override
    public List<Integer> generate(Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule) {
        int expectedCustomerCount = 0;
        for (GameDayStartResponse.HourlySchedule schedule : hourlySchedule.values()) {
            expectedCustomerCount += schedule.population();
        }

        List<Integer> purchaseList = new ArrayList<>(expectedCustomerCount);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < expectedCustomerCount; index++) {
            purchaseList.add(drawPurchaseQuantity(random.nextInt(100)));
        }
        return purchaseList;
    }

    private int drawPurchaseQuantity(int roll) {
        int cumulative = 0;
        for (int quantity = 0; quantity < PURCHASE_QUANTITY_WEIGHTS.length; quantity++) {
            cumulative += PURCHASE_QUANTITY_WEIGHTS[quantity];
            if (roll < cumulative) {
                return quantity;
            }
        }
        return PURCHASE_QUANTITY_WEIGHTS.length - 1;
    }
}
