package com.ssafy.S14P21A205.game.day.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GameDayStartRequest(
        @NotNull(message = "locationId는 필수입니다.") Long locationId,
        @NotNull(message = "menuId는 필수입니다.") Long menuId,
        @NotNull(message = "price는 필수입니다.")
        @Positive(message = "price는 0보다 커야 합니다.")
        Integer price,
        @NotNull(message = "orderCount는 필수입니다.")
        @PositiveOrZero(message = "orderCount는 0 이상이어야 합니다.")
        Integer orderCount
) {
}
