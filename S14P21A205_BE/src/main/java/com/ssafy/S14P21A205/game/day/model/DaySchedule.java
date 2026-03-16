package com.ssafy.S14P21A205.game.day.model;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.math.BigDecimal;
import java.util.Map;

public record DaySchedule(
        Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule,
        BigDecimal dailyTrafficMultiplier
) {
}
