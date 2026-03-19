package com.ssafy.S14P21A205.game.time.model;

import java.time.LocalDateTime;

public record GameTimePoint(
        LocalDateTime occurredAt,
        GamePhase phase,
        long elapsedBusinessSeconds
) {
}
