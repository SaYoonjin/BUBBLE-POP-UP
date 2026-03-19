package com.ssafy.S14P21A205.game.news.dto;

public record AreaRankingItemResponse(
        int rank,
        String areaName,
        double changeRate
) {
}
