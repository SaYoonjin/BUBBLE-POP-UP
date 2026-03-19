package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "긴급 발주 요청")
public record EmergencyOrderRequest(
        @Schema(description = "메뉴 ID", example = "7")
        @NotNull @Min(1) Integer menuId,
        @Schema(description = "발주 수량", example = "100")
        @NotNull @Min(1) Integer quantity,
        @Schema(description = "도착 후 적용할 판매가", example = "4200")
        @NotNull @Min(1) Integer salePrice
) {
}
