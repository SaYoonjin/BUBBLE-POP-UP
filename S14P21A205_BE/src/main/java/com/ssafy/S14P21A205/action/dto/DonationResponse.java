package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "나눔 이벤트 응답")
public record DonationResponse(
        @Schema(description = "나눔 수량") int quantity,
        @Schema(description = "유입률 보너스") BigDecimal captureRateBonus,
        @Schema(description = "결과 메시지") String message
) {
}
