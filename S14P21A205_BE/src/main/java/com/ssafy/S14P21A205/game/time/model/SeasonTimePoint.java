package com.ssafy.S14P21A205.game.time.model;

import java.time.LocalDateTime;

public record SeasonTimePoint(
        LocalDateTime occurredAt,
        SeasonPhase phase,
        Integer currentDay,
        LocalDateTime phaseStartAt,
        LocalDateTime phaseEndAt,
        long elapsedPhaseSeconds,
        long remainingPhaseSeconds,
        long elapsedBusinessSeconds,
        String gameTime,
        Integer tick,
        boolean joinEnabled,
        Integer joinPlayableFromDay,
        LocalDateTime nextSeasonStartAt
) {
    public boolean isPlayableDayPhase() {
        return phase == SeasonPhase.DAY_PREPARING
                || phase == SeasonPhase.DAY_BUSINESS
                || phase == SeasonPhase.DAY_REPORT;
    }
}

