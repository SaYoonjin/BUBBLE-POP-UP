package com.ssafy.S14P21A205.game.day.dto;

import java.util.List;

public record GameDaySnapshot(
        Long storeId,
        Long seasonId,
        Integer day,
        Long locationId,
        Long menuId,
        Integer price,
        Integer orderCount,
        Long dailySeed,
        List<Integer> purchaseList,
        Integer purchaseCursor,
        GameDayStartResponse response
) {
}
