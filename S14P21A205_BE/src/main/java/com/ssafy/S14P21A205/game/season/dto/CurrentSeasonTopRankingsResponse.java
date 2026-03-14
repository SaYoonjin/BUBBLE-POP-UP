package com.ssafy.S14P21A205.game.season.dto;

import java.util.List;

// 실시간 TOP 응답
public record CurrentSeasonTopRankingsResponse(
        Long seasonId,
        List<CurrentSeasonTopRankingItemResponse> rankings,
        String refreshedAt
) {
}
