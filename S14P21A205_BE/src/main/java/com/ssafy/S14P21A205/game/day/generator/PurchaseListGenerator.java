package com.ssafy.S14P21A205.game.day.generator;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.util.Map;

public interface PurchaseListGenerator {

    java.util.List<Integer> generate(Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule);
}
