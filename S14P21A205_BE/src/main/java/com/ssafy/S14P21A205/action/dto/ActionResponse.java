package com.ssafy.S14P21A205.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "액션 실행 응답")
public record ActionResponse(
        @Schema(description = "액션 타입") String actionType,
        @Schema(description = "비용") int cost,
        @Schema(description = "결과 메시지") String message
) {
}
