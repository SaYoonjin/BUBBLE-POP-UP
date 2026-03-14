package com.ssafy.S14P21A205.game.day.dto;

import java.time.LocalDateTime;

public record GameDayLiveState(
        long cumulativeSales,
        long cumulativeTotalCost,
        Integer tick,
        LocalDateTime lastCalculatedAt
) {
}