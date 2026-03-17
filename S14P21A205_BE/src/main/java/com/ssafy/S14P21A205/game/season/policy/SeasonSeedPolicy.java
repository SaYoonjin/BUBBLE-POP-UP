package com.ssafy.S14P21A205.game.season.policy;

import com.ssafy.S14P21A205.game.season.entity.SeasonSeed;
import java.time.LocalDateTime;

public class SeasonSeedPolicy {

    private static final int DEFAULT_TOTAL_DAYS = 7;

    public SeasonSeed issue(LocalDateTime startTime, LocalDateTime endTime) {
        return new SeasonSeed(DEFAULT_TOTAL_DAYS, startTime, endTime);
    }
}
