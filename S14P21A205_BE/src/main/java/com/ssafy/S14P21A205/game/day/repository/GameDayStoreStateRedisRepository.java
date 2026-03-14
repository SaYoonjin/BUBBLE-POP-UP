package com.ssafy.S14P21A205.game.day.repository;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class GameDayStoreStateRedisRepository {

    private static final String STATE_KEY_PATTERN = "game:store:%d:day:%d:state";
    private static final String FIELD_CUMULATIVE_SALES = "cumulative_sales";
    private static final String FIELD_CUMULATIVE_TOTAL_COST = "cumulative_total_cost";
    private static final String FIELD_TICK = "tick";
    private static final String FIELD_LAST_CALCULATED_AT = "last_calculated_at";

    private final StringRedisTemplate stringRedisTemplate;

    public Optional<GameDayLiveState> find(Long storeId, Integer day) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(buildStateKey(storeId, day));
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new GameDayLiveState(
                    parseLong(entries.get(FIELD_CUMULATIVE_SALES)),
                    parseLong(entries.get(FIELD_CUMULATIVE_TOTAL_COST)),
                    parseInteger(entries.get(FIELD_TICK)),
                    parseLocalDateTime(entries.get(FIELD_LAST_CALCULATED_AT))
            ));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    String buildStateKey(Long storeId, Integer day) {
        return STATE_KEY_PATTERN.formatted(storeId, day);
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return 0L;
        }
        return Long.parseLong(text);
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return LocalDateTime.parse(text);
    }
}