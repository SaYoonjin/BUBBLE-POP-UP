package com.ssafy.S14P21A205.game.day.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameDayStartResponse(
        String startTime,
        String endTime,
        Map<String, HourlySchedule> hourlySchedule,
        String weatherType,
        BigDecimal weatherMultiplier,
        BigDecimal trafficSchedule,
        BigDecimal captureRate,
        List<EventSchedule> eventSchedule,
        Integer initialBalance,
        Integer initialStock
) {
    public record HourlySchedule(
            Integer population,
            BigDecimal trafficMultiplier
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventSchedule(
            String time,
            String type,
            Scope scope,
            String newsTitle,
            BigDecimal populationMultiplier,
            Integer balanceChange
    ) {
    }

    public record Scope(
            Long region,
            Long menu
    ) {
    }
}
