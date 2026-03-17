package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import java.util.List;

public class EnvironmentScheduleResolver {

    public ResolvedEnvironment resolve(
            PopulationPolicy populationPolicy,
            WeatherRepository weatherRepository,
            Long locationId,
            int day
    ) {
        DaySchedule daySchedule = populationPolicy.buildDaySchedule(locationId, day);
        List<Weather> weathers = weatherRepository.findAllByOrderByIdAsc();
        if (weathers.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Weather weather = weathers.get(Math.floorMod(day - 1, weathers.size()));
        return new ResolvedEnvironment(daySchedule, weather);
    }

    public record ResolvedEnvironment(
            DaySchedule daySchedule,
            Weather weather
    ) {
    }
}
