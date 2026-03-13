package com.ssafy.S14P21A205.game.state.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 기반 게임 실시간 상태 저장소.
 *
 * <p>관리하는 키 (표준안 적용):
 * <ul>
 *   <li>{@code game:store:{storeId}:stock} — 재고 (String)</li>
 *   <li>{@code game:store:{storeId}:balance} — 잔액 (String)</li>
 *   <li>{@code game:store:{storeId}:price} — 판매가 (String)</li>
 *   <li>{@code game:store:{storeId}:capture-rate} — 유입률 (String)</li>
 *   <li>{@code game:store:{storeId}:day:{day}:actions} — 일차별 액션 사용 여부 (JSON)</li>
 *   <li>{@code game:store:{storeId}:emergency} — 긴급발주 임시 데이터 (JSON)</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class GameStateRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String STOCK_KEY = "game:store:%d:stock";
    private static final String BALANCE_KEY = "game:store:%d:balance";
    private static final String PRICE_KEY = "game:store:%d:price";
    private static final String CAPTURE_RATE_KEY = "game:store:%d:capture-rate";
    private static final String ACTIONS_KEY = "game:store:%d:day:%d:actions";
    private static final String EMERGENCY_KEY = "game:store:%d:emergency";

    // ── game:store:{storeId}:capture-rate ──

    public BigDecimal getCaptureRate(Long storeId) {
        String val = redisTemplate.opsForValue().get(captureRateKey(storeId));
        return val == null ? null : new BigDecimal(val);
    }

    public void setCaptureRate(Long storeId, BigDecimal captureRate) {
        redisTemplate.opsForValue().set(captureRateKey(storeId),
                captureRate.setScale(4, RoundingMode.HALF_UP).toPlainString());
    }

    // ── game:store:{storeId}:price ──

    public Integer getPrice(Long storeId) {
        String val = redisTemplate.opsForValue().get(priceKey(storeId));
        return val == null ? null : Integer.parseInt(val);
    }

    public void setPrice(Long storeId, int price) {
        redisTemplate.opsForValue().set(priceKey(storeId), String.valueOf(price));
    }

    // ── game:store:{storeId}:stock ──

    public Integer getStock(Long storeId) {
        String val = redisTemplate.opsForValue().get(stockKey(storeId));
        return val == null ? null : Integer.parseInt(val);
    }

    public void setStock(Long storeId, int stock) {
        redisTemplate.opsForValue().set(stockKey(storeId), String.valueOf(stock));
    }

    // ── game:store:{storeId}:balance ──

    public Integer getBalance(Long storeId) {
        String val = redisTemplate.opsForValue().get(balanceKey(storeId));
        return val == null ? null : Integer.parseInt(val);
    }

    public void setBalance(Long storeId, int balance) {
        redisTemplate.opsForValue().set(balanceKey(storeId), String.valueOf(balance));
    }

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

    // ── game:store:{storeId}:emergency ──

    public void setEmergencyOrder(Long storeId, Map<String, String> data) {
        try {
            redisTemplate.opsForValue().set(emergencyKey(storeId),
                    objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize emergency order", e);
        }
    }

    public Map<String, String> getEmergencyOrder(Long storeId) {
        String json = redisTemplate.opsForValue().get(emergencyKey(storeId));
        if (json == null) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public void deleteEmergencyOrder(Long storeId) {
        redisTemplate.delete(emergencyKey(storeId));
    }

    // ── Key builders ──

    private String stockKey(Long storeId) {
        return String.format(STOCK_KEY, storeId);
    }

    private String balanceKey(Long storeId) {
        return String.format(BALANCE_KEY, storeId);
    }

    private String priceKey(Long storeId) {
        return String.format(PRICE_KEY, storeId);
    }

    private String captureRateKey(Long storeId) {
        return String.format(CAPTURE_RATE_KEY, storeId);
    }

    private String actionsKey(Long storeId, int day) {
        return String.format(ACTIONS_KEY, storeId, day);
    }

    private String emergencyKey(Long storeId) {
        return String.format(EMERGENCY_KEY, storeId);
    }
}
