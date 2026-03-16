package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "할인 이벤트 요청")
public record DiscountRequest(
        @Schema(description = "할인 금액 (원)", example = "500")
        @NotNull @Min(1) Integer discountValue
) {
}
