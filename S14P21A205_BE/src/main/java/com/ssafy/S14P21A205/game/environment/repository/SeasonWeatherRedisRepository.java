package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class SeasonWeatherRedisRepository {

    private static final String WEATHER_SCHEDULE_KEY_PATTERN = "season:%d:weather_schedule";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<List<SeasonWeatherEntry>> findSchedule(Long seasonId) {
        String payload = stringRedisTemplate.opsForValue().get(buildKey(seasonId));
        if (!StringUtils.hasText(payload)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(payload, new TypeReference<>() {
            }));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public Optional<SeasonWeatherEntry> findDay(Long seasonId, int day) {
        return findSchedule(seasonId)
                .flatMap(schedule -> schedule.stream()
                        .filter(entry -> entry.day() != null && entry.day() == day)
                        .findFirst());
    }

    public void saveSchedule(Long seasonId, List<SeasonWeatherEntry> schedule) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(seasonId), objectMapper.writeValueAsString(schedule));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String buildKey(Long seasonId) {
        return WEATHER_SCHEDULE_KEY_PATTERN.formatted(seasonId);
    }

    public record SeasonWeatherEntry(
            Integer day,
            WeatherType weatherType,
            BigDecimal populationMultiplier
    ) {
    }
}
