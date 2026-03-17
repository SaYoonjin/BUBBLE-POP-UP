package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.SeasonWeatherRedisRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvironmentScheduleResolver {

    private final PopulationPolicy populationPolicy;
    private final SeasonWeatherRedisRepository seasonWeatherRedisRepository;

    public ResolvedEnvironment resolve(Long seasonId, Long locationId, int day) {
        SeasonWeatherRedisRepository.SeasonWeatherEntry weather = resolveWeather(seasonId, day);
        DaySchedule daySchedule = populationPolicy.buildDaySchedule(
                locationId,
                day,
                normalizeScale(weather.populationMultiplier())
        );
        return new ResolvedEnvironment(daySchedule, weather.weatherType(), normalizeScale(weather.populationMultiplier()));
    }

    private SeasonWeatherRedisRepository.SeasonWeatherEntry resolveWeather(Long seasonId, int day) {
        return seasonWeatherRedisRepository.findDay(seasonId, day)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record ResolvedEnvironment(
            DaySchedule daySchedule,
            WeatherType weatherType,
            BigDecimal weatherMultiplier
    ) {
    }
}
