package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "액션 사용 현황 응답")
public record ActionStatusResponse(
        @Schema(description = "할인 사용 여부") boolean discountUsed,
        @Schema(description = "긴급발주 사용 여부") boolean emergencyUsed,
        @Schema(description = "나눔 사용 여부") boolean donationUsed,
        @Schema(description = "인플루언서 홍보 사용 여부") boolean influencerUsed,
        @Schema(description = "SNS 홍보 사용 여부") boolean snsUsed,
        @Schema(description = "전단지 홍보 사용 여부") boolean leafletUsed,
        @Schema(description = "지인소개 홍보 사용 여부") boolean friendUsed
) {
    public static ActionStatusResponse from(Map<String, Boolean> actions) {
        return new ActionStatusResponse(
                Boolean.TRUE.equals(actions.get("discount")),
                Boolean.TRUE.equals(actions.get("emergency")),
                Boolean.TRUE.equals(actions.get("donation")),
                Boolean.TRUE.equals(actions.get("influencer")),
                Boolean.TRUE.equals(actions.get("sns")),
                Boolean.TRUE.equals(actions.get("leaflet")),
                Boolean.TRUE.equals(actions.get("friend"))
        );
    }
}
