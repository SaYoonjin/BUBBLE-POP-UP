package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.environment.entity.WeatherLocation;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.WeatherDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherLocationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvironmentScheduleResolver {

    private final PopulationPolicy populationPolicy;
    private final WeatherDayRedisRepository weatherDayRedisRepository;
    private final WeatherLocationRepository weatherLocationRepository;

    public ResolvedEnvironment resolve(Long seasonId, Long locationId, int day) {
        WeatherDayRedisRepository.WeatherDayEntry weather = resolveWeather(seasonId, locationId, day);
        DaySchedule daySchedule = populationPolicy.buildDaySchedule(
                locationId,
                day,
                normalizeScale(weather.populationMultiplier())
        );
        return new ResolvedEnvironment(daySchedule, weather.weatherType(), normalizeScale(weather.populationMultiplier()));
    }

    private WeatherDayRedisRepository.WeatherDayEntry resolveWeather(Long seasonId, Long locationId, int day) {
        return weatherDayRedisRepository.findLocation(seasonId, locationId, day)
                .or(() -> loadAndCacheDay(seasonId, locationId, day))
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private java.util.Optional<WeatherDayRedisRepository.WeatherDayEntry> loadAndCacheDay(Long seasonId, Long locationId, int day) {
        List<WeatherLocation> dayEntries = weatherLocationRepository.findByDayOrderByLocation_IdAsc(day);
        if (dayEntries.isEmpty()) {
            return java.util.Optional.empty();
        }

        List<WeatherDayRedisRepository.WeatherDayEntry> cachedEntries = dayEntries.stream()
                .map(entry -> new WeatherDayRedisRepository.WeatherDayEntry(
                        entry.getLocation().getId(),
                        entry.getDay(),
                        entry.getWeather().getWeatherType(),
                        entry.getWeather().getPopulationPercent()
                ))
                .toList();
        weatherDayRedisRepository.saveDay(seasonId, day, cachedEntries);
        return cachedEntries.stream()
                .filter(entry -> locationId.equals(entry.locationId()))
                .findFirst();
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
