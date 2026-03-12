package com.ssafy.S14P21A205.game.day.repository;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDaySnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class GameDaySnapshotRedisRepository {

    private static final String KEY_PREFIX = "game:day:start";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Integer userId, Long seasonId, Integer day, GameDaySnapshot snapshot) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(userId, seasonId, day), objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public Optional<GameDaySnapshot> find(Integer userId, Long seasonId, Integer day) {
        String payload = stringRedisTemplate.opsForValue().get(buildKey(userId, seasonId, day));
        if (!StringUtils.hasText(payload)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(payload, GameDaySnapshot.class));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void delete(Integer userId, Long seasonId, Integer day) {
        stringRedisTemplate.delete(buildKey(userId, seasonId, day));
    }

    String buildKey(Integer userId, Long seasonId, Integer day) {
        return "%s:%s:%d:%d".formatted(KEY_PREFIX, userId, seasonId, day);
    }
}
