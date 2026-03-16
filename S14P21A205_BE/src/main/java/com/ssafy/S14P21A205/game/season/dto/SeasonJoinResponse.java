package com.ssafy.S14P21A205.game.season.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SeasonJoinResponse(
        Long storeId,
        String storeName,
        Integer balance
) {
}
