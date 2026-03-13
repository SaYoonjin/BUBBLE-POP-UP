package com.ssafy.S14P21A205.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegularOrderRequest(

        @NotNull(message = "menuId는 필수입니다.")
        Integer menuId,

        @NotNull(message = "quantity는 필수입니다.")
        @Min(value = 50, message = "발주 수량은 50 이상이어야 합니다.")
        @Max(value = 500, message = "발주 수량은 500 이하여야 합니다.")
        Integer quantity,

        @Positive(message = "price는 0보다 커야 합니다.")
        Integer price
) {
}
