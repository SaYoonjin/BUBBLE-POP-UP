package com.ssafy.S14P21A205.game.season.dto;

public record GameWaitingResponse(
        GameWaitingStatus status,
        Integer nextSeasonNumber,
        Integer currentDay,
        Integer nextSeasonStartTime,
        String seasonPhase,
        Integer phaseRemainingSeconds,
        String gameTime,
        Integer tick,
        Boolean joinEnabled,
        Integer joinPlayableFromDay,
        Integer participantCount
) {
    public GameWaitingResponse(
            GameWaitingStatus status,
            Integer nextSeasonNumber,
            Integer currentDay,
            Integer nextSeasonStartTime
    ) {
        this(status, nextSeasonNumber, currentDay, nextSeasonStartTime, null, null, null, null, null, null, null);
    }
}

