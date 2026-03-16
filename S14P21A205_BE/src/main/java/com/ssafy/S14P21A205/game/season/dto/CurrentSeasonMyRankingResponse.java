package com.ssafy.S14P21A205.game.season.dto;

import java.math.BigDecimal;

// 최종 랭킹 안의 내 순위 전용
public record CurrentSeasonMyRankingResponse(
        Integer rank,
        String nickname,
        String storeName,
        String locationName,
        String menuName,
        BigDecimal roi,
        Long totalRevenue,
        Integer rewardPoints
) {
}
