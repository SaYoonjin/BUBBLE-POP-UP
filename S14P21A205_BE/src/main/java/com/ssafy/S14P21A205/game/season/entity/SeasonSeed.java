package com.ssafy.S14P21A205.game.season.entity;

import java.time.LocalDateTime;

public record SeasonSeed(
        int totalDays,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
