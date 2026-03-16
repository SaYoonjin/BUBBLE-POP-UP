package com.ssafy.S14P21A205.action.dto;

import com.ssafy.S14P21A205.action.entity.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "홍보 실행 요청")
public record PromotionRequest(
        @Schema(description = "홍보 종류", example = "INFLUENCER")
        @NotNull PromotionType promotionType
) {
}
