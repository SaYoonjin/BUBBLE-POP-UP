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
    public static ActionStatusResponse from(Map<Object, Object> actions) {
        return new ActionStatusResponse(
                "true".equals(str(actions.get("discount"))),
                "true".equals(str(actions.get("emergency"))),
                "true".equals(str(actions.get("donation"))),
                "true".equals(str(actions.get("influencer"))),
                "true".equals(str(actions.get("sns"))),
                "true".equals(str(actions.get("leaflet"))),
                "true".equals(str(actions.get("friend")))
        );
    }

    private static String str(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
