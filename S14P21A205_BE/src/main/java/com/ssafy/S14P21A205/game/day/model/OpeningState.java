package com.ssafy.S14P21A205.game.day.model;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;

public record OpeningState(
        int initialBalance,
        int initialStock,
        GameDayStartResponse.OpeningSummary openingSummary
) {
}
