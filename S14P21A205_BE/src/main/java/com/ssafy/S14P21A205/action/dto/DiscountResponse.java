package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "할인 이벤트 응답")
public record DiscountResponse(
        @Schema(description = "이전 판매가") int previousPrice,
        @Schema(description = "할인 후 판매가") int newPrice,
        @Schema(description = "가격 구간 (ABOVE/AVERAGE/BELOW)") String priceRange,
        @Schema(description = "유입률 배수 (0.8/1.0/1.2)") BigDecimal priceRangeMultiplier,
        @Schema(description = "결과 메시지") String message
) {
}
