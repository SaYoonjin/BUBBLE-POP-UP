package com.ssafy.S14P21A205.game.state.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 액션 사용 현황 Redis 저장소.
 *
 * <p>관리하는 키:
 * <ul>
 *   <li>{@code game:store:{storeId}:day:{day}:actions} — 일차별 액션 사용 여부 (JSON)</li>
 * </ul>
 *
 * <p>잔액·재고·판매가 등 게임 실시간 상태는
 * {@code GameDayStoreStateRedisRepository}의 Hash({@code game:store:{storeId}:day:{day}:state})에서 관리.
 * 긴급발주 상태는 {@code Order} 엔티티(DB)에서 관리.
 */
@Repository
@RequiredArgsConstructor
public class GameStateRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ACTIONS_KEY = "game:store:%d:day:%d:actions";

    // ── game:store:{storeId}:day:{day}:actions ──

    public Map<String, Boolean> getActions(Long storeId, int day) {
        String json = redisTemplate.opsForValue().get(actionsKey(storeId, day));
        if (json == null) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public boolean isActionUsed(Long storeId, int day, String actionField) {
        return Boolean.TRUE.equals(getActions(storeId, day).get(actionField));
    }

    public void markActionUsed(Long storeId, int day, String actionField) {
        Map<String, Boolean> actions = getActions(storeId, day);
        actions.put(actionField, true);
        try {
            redisTemplate.opsForValue().set(actionsKey(storeId, day),
                    objectMapper.writeValueAsString(actions));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize actions", e);
        }
    }

    // ── Key builders ──

    private String actionsKey(Long storeId, int day) {
        return String.format(ACTIONS_KEY, storeId, day);
    }
}
