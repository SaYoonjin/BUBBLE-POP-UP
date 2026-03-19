package com.ssafy.S14P21A205.game.season.dto;

public record SeasonJoinResponse(
        Long storeId,
        String storeName,
        Integer balance,
        Integer playableFromDay,
        Boolean waitingForPlayableDay
) {
    public SeasonJoinResponse(Long storeId, String storeName, Integer balance) {
        this(storeId, storeName, balance, 1, false);
    }
}

