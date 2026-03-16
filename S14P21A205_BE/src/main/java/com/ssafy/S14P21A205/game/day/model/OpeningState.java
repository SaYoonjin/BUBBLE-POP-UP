package com.ssafy.S14P21A205.game.day.model;

public record OpeningState(
        int initialBalance,
        int initialStock,
        int orderCount,
        int orderCost
) {
}
