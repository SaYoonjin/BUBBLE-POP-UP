package com.ssafy.S14P21A205.game.season.dto;

import java.util.List;

// 최종 랭킹 전체 응답
public record CurrentSeasonRankingsResponse(
        Long seasonId,
        List<CurrentSeasonRankingItemResponse> rankings,
        List<CurrentSeasonRankingItemResponse> myRankings
) {
}
