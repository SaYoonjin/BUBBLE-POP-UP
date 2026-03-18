package com.ssafy.S14P21A205.game.day.generator;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class RandomPurchaseListGenerator implements PurchaseListGenerator {

    private static final int[] PURCHASE_QUANTITY_WEIGHTS = {10, 40, 35, 15};
    private static final int SEASON_QUEUE_SIZE = 20_000;

    @Override
    public List<Integer> generate(
            Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule,
            Long purchaseSeed,
            Integer seasonCursor
    ) {
        int expectedCustomerCount = expectedCustomerCount(hourlySchedule);
        if (expectedCustomerCount <= 0) {
            return List.of();
        }

        List<Integer> seasonQueue = buildSeasonQueue(resolveSeed(purchaseSeed));
        int cursor = normalizeCursor(seasonCursor);
        List<Integer> purchaseList = new ArrayList<>(expectedCustomerCount);
        for (int index = 0; index < expectedCustomerCount; index++) {
            purchaseList.add(seasonQueue.get((cursor + index) % SEASON_QUEUE_SIZE));
        }
        return purchaseList;
    }

    @Override
    public long issueSeed() {
        return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    @Override
    public int advanceCursor(Integer currentCursor, int consumedCount) {
        int cursor = normalizeCursor(currentCursor);
        if (consumedCount <= 0) {
            return cursor;
        }
        return (cursor + consumedCount) % SEASON_QUEUE_SIZE;
    }

    @Override
    public int normalizeCursor(Integer cursor) {
        if (cursor == null) {
            return 0;
        }
        return Math.floorMod(cursor, SEASON_QUEUE_SIZE);
    }

    private int expectedCustomerCount(Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule) {
        if (hourlySchedule == null || hourlySchedule.isEmpty()) {
            return 0;
        }

        int expectedCustomerCount = 0;
        for (GameDayStartResponse.HourlySchedule schedule : hourlySchedule.values()) {
            expectedCustomerCount += schedule.population();
        }
        return Math.max(expectedCustomerCount, 0);
    }

    private List<Integer> buildSeasonQueue(long purchaseSeed) {
        List<Integer> seasonQueue = new ArrayList<>(SEASON_QUEUE_SIZE);
        Random random = new Random(purchaseSeed);
        for (int index = 0; index < SEASON_QUEUE_SIZE; index++) {
            seasonQueue.add(drawPurchaseQuantity(random.nextInt(100)));
        }
        return seasonQueue;
    }

    private long resolveSeed(Long purchaseSeed) {
        return purchaseSeed == null ? 0L : purchaseSeed;
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
