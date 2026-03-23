package com.ssafy.S14P21A205.game.news.dto;

import java.util.List;

public record NewsRankingResponse(
        List<AreaRankingItemResponse> areaRevenueRanking,
        List<AreaRankingItemResponse> areaTrafficRanking
) {
}
