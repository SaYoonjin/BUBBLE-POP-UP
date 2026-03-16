package com.ssafy.S14P21A205.game.season.dto;

public record GameWaitingResponse(
        GameWaitingStatus status,
        Integer nextSeasonNumber,
        Integer currentDay,
        Integer nextSeasonStartTime
) {
}
