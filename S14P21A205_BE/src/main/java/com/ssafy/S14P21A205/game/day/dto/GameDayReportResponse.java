package com.ssafy.S14P21A205.game.day.dto;

public record GameDayReportResponse(
        Long seasonId,
        Integer day,
        String locationName,
        String menuName,
        Long revenue,
        Long totalCost,
        Long netProfit,
        Integer visitors,
        Integer salesCount,
        Integer stockRemaining,
        Integer stockDisposedCount,
        TomorrowWeather tomorrowWeather,
        Boolean isNextDayOrderDay,
        Integer consecutiveDeficitDays,
        Boolean isBankrupt
) {
    public record TomorrowWeather(
            String condition
    ) {
    }
}
