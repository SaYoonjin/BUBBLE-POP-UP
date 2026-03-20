package com.ssafy.S14P21A205.game.season.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Current season global time state")
public record CurrentSeasonTimeResponse(
        @Schema(description = "Current season phase", example = "DAY_PREPARING")
        String seasonPhase,
        @Schema(description = "Current progressing day", example = "3")
        int currentDay,
        @Schema(description = "Remaining seconds in the current phase", example = "12")
        int phaseRemainingSeconds,
        @Schema(description = "Current server time", example = "2026-03-18T12:05:10")
        LocalDateTime serverTime,
        @Schema(description = "Season start time", example = "2026-03-18T12:00:00")
        LocalDateTime seasonStartTime,
        @Schema(description = "Current game time during business phase", example = "14:00")
        String gameTime,
        @Schema(description = "Current tick during business phase", example = "4")
        Integer tick,
        @Schema(description = "Whether joining the current season is enabled", example = "true")
        boolean joinEnabled,
        @Schema(description = "Playable start day when joined now", example = "4")
        Integer joinPlayableFromDay
) {
}

