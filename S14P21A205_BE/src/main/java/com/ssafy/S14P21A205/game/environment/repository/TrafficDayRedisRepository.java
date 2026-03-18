package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class TrafficDayRedisRepository {

    private static final String TRAFFIC_DAY_KEY_PATTERN = "traffic:location:%d:date:%s";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<List<TrafficEntry>> findDay(Long locationId, LocalDate date) {
        String payload = stringRedisTemplate.opsForValue().get(buildKey(locationId, date));
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

    public Optional<TrafficEntry> findExact(Long locationId, LocalDateTime dateTime) {
        return findDay(locationId, dateTime.toLocalDate())
                .flatMap(entries -> entries.stream()
                        .filter(entry -> dateTime.equals(entry.dateTime()))
                        .findFirst());
    }

    public void saveDay(Long locationId, LocalDate date, List<TrafficEntry> entries) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(locationId, date), objectMapper.writeValueAsString(entries));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String buildKey(Long locationId, LocalDate date) {
        return TRAFFIC_DAY_KEY_PATTERN.formatted(locationId, date);
    }

    public record TrafficEntry(
            LocalDateTime dateTime,
            TrafficStatus trafficStatus
    ) {
    }
}
