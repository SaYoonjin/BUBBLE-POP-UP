package com.ssafy.S14P21A205.game.day.generator;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.util.List;
import java.util.Map;

public interface PurchaseListGenerator {

    List<Integer> generate(
            Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule,
            Long purchaseSeed,
            Integer seasonCursor
    );

    long issueSeed();

    int advanceCursor(Integer currentCursor, int consumedCount);

    int normalizeCursor(Integer cursor);
}
