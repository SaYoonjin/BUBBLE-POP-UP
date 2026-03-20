package com.ssafy.S14P21A205.game.season.dto;

public record SeasonSummaryResponse(
        Long seasonId,
        Integer finalRank,
        StoreInfo storeInfo,
        BusinessRecord businessRecord
) {
    public record StoreInfo(
            String storeName,
            String locationName,
            String menuName
    ) {
    }

    public record BusinessRecord(
            Long totalRevenue,
            Long totalCost,
            Long totalNetProfit,
            Integer totalVisitors,
            Double roi,
            Integer daysPlayed
    ) {
    }
}
