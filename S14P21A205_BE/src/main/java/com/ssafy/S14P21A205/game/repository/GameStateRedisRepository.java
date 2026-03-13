package com.ssafy.S14P21A205.game.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GameStateRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String STATE_KEY = "game:state:%d";
    private static final String ACTIONS_KEY = "game:actions:%d:%d";
    private static final String EMERGENCY_KEY = "game:emergency:%d";

    // === game:state:{storeId} - 가게 실시간 상태 (유입률, 가격, 재고, 잔액)

    public BigDecimal getCaptureRate(Long storeId) {
        Object val = redisTemplate.opsForHash().get(stateKey(storeId), "captureRate");
        return val == null ? null : new BigDecimal(val.toString());
    }

    public Integer getPrice(Long storeId) {
        Object val = redisTemplate.opsForHash().get(stateKey(storeId), "price");
        return val == null ? null : Integer.parseInt(val.toString());
    }

    public Integer getStock(Long storeId) {
        Object val = redisTemplate.opsForHash().get(stateKey(storeId), "stock");
        return val == null ? null : Integer.parseInt(val.toString());
    }

    public Integer getBalance(Long storeId) {
        Object val = redisTemplate.opsForHash().get(stateKey(storeId), "balance");
        return val == null ? null : Integer.parseInt(val.toString());
    }

    public void setCaptureRate(Long storeId, BigDecimal captureRate) {
        redisTemplate.opsForHash().put(stateKey(storeId), "captureRate",
                captureRate.setScale(4, RoundingMode.HALF_UP).toPlainString());
    }

    public void setPrice(Long storeId, int price) {
        redisTemplate.opsForHash().put(stateKey(storeId), "price", String.valueOf(price));
    }

    public void setStock(Long storeId, int stock) {
        redisTemplate.opsForHash().put(stateKey(storeId), "stock", String.valueOf(stock));
    }

    public void setBalance(Long storeId, int balance) {
        redisTemplate.opsForHash().put(stateKey(storeId), "balance", String.valueOf(balance));
    }

    // === game:actions:{storeId}:{day} - 일차별 액션 사용 현황

    public Map<Object, Object> getActions(Long storeId, int day) {
        return redisTemplate.opsForHash().entries(actionsKey(storeId, day));
    }

    public boolean isActionUsed(Long storeId, int day, String actionField) {
        Object val = redisTemplate.opsForHash().get(actionsKey(storeId, day), actionField);
        return "true".equals(val == null ? null : val.toString());
    }

    public void markActionUsed(Long storeId, int day, String actionField) {
        redisTemplate.opsForHash().put(actionsKey(storeId, day), actionField, "true");
    }

    // === game:emergency:{storeId} - 긴급발주 임시 데이터

    public void setEmergencyOrder(Long storeId, Map<String, String> data) {
        redisTemplate.opsForHash().putAll(emergencyKey(storeId), data);
    }

    public Map<Object, Object> getEmergencyOrder(Long storeId) {
        return redisTemplate.opsForHash().entries(emergencyKey(storeId));
    }

    public void deleteEmergencyOrder(Long storeId) {
        redisTemplate.delete(emergencyKey(storeId));
    }

    // === Key builders

    private String stateKey(Long storeId) {
        return String.format(STATE_KEY, storeId);
    }

    private String actionsKey(Long storeId, int day) {
        return String.format(ACTIONS_KEY, storeId, day);
    }

    private String emergencyKey(Long storeId) {
        return String.format(EMERGENCY_KEY, storeId);
    }
}