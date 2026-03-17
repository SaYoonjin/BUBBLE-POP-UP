package com.ssafy.S14P21A205.game.day.state.repository;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class GameDayStoreStateRedisRepository {

    private static final String STATE_KEY_PATTERN = "game:store:%d:day:%d:state";
    private static final String TICK_LOG_KEY_PATTERN = "game:store:%d:day:%d:tick_log";
    private static final String ACTIONS_KEY_PATTERN = "game:store:%d:day:%d:actions";
    private static final String FIELD_STARTED_AT = "started_at";
    private static final String FIELD_PURCHASE_LIST = "purchase_list";
    private static final String FIELD_PURCHASE_CURSOR = "purchase_cursor";
    private static final String FIELD_START_RESPONSE = "start_response";
    private static final String FIELD_POPULATION_PER_STORE = "population_per_store";
    private static final String FIELD_CAPTURE_RATE = "capture_rate";
    private static final String LEGACY_FIELD_CAPTURE_RATE = "inflow_rate";
    private static final String FIELD_SALE_PRICE = "sale_price";
    private static final String FIELD_TICK_CUSTOMER_COUNT = "tick_customer_count";
    private static final String FIELD_TICK_PURCHASE_COUNT = "tick_purchase_count";
    private static final String FIELD_TICK_SALES = "tick_sales";
    private static final String FIELD_CUMULATIVE_CUSTOMER_COUNT = "cumulative_customer_count";
    private static final String FIELD_CUMULATIVE_PURCHASE_COUNT = "cumulative_purchase_count";
    private static final String FIELD_CUMULATIVE_SALES = "cumulative_sales";
    private static final String FIELD_CUMULATIVE_TOTAL_COST = "cumulative_total_cost";
    private static final String FIELD_BALANCE = "balance";
    private static final String FIELD_STOCK = "stock";
    private static final String FIELD_TICK = "tick";
    private static final String FIELD_LAST_CALCULATED_AT = "last_calculated_at";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<GameDayLiveState> find(Long storeId, Integer day) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(buildStateKey(storeId, day));
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new GameDayLiveState(
                    parseLocalDateTime(entries.get(FIELD_STARTED_AT)),
                    parsePurchaseList(entries.get(FIELD_PURCHASE_LIST)),
                    parseInteger(entries.get(FIELD_PURCHASE_CURSOR)),
                    parseStartResponse(entries.get(FIELD_START_RESPONSE)),
                    parseInteger(entries.get(FIELD_TICK)),
                    parseInteger(entries.get(FIELD_POPULATION_PER_STORE)),
                    parseCaptureRate(entries),
                    parseInteger(entries.get(FIELD_SALE_PRICE)),
                    parseInteger(entries.get(FIELD_TICK_CUSTOMER_COUNT)),
                    parseInteger(entries.get(FIELD_TICK_PURCHASE_COUNT)),
                    parseLongObject(entries.get(FIELD_TICK_SALES)),
                    parseInteger(entries.get(FIELD_CUMULATIVE_CUSTOMER_COUNT)),
                    parseInteger(entries.get(FIELD_CUMULATIVE_PURCHASE_COUNT)),
                    parseLongObject(entries.get(FIELD_CUMULATIVE_SALES)),
                    parseLongObject(entries.get(FIELD_CUMULATIVE_TOTAL_COST)),
                    parseLongObject(entries.get(FIELD_BALANCE)),
                    parseInteger(entries.get(FIELD_STOCK)),
                    parseLocalDateTime(entries.get(FIELD_LAST_CALCULATED_AT))
            ));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void save(Long storeId, Integer day, GameDayLiveState state) {
        try {
            stringRedisTemplate.opsForHash().putAll(buildStateKey(storeId, day), buildEntries(state));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void saveStateAndTickLog(Long storeId, Integer day, GameDayLiveState state) {
        try {
            stringRedisTemplate.opsForHash().putAll(buildStateKey(storeId, day), buildLiveEntries(state));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
        saveTickLog(storeId, day, state);
    }

    public void updateField(Long storeId, Integer day, String field, String value) {
        stringRedisTemplate.opsForHash().put(buildStateKey(storeId, day), field, value);
    }

    public boolean exists(Long storeId, Integer day) {
        Boolean exists = stringRedisTemplate.hasKey(buildStateKey(storeId, day));
        return Boolean.TRUE.equals(exists);
    }

    public Optional<Long> findBalance(Long storeId, Integer day) {
        try {
            return Optional.ofNullable(parseLongObject(
                    stringRedisTemplate.opsForHash().get(buildStateKey(storeId, day), FIELD_BALANCE)
            ));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void saveBalance(Long storeId, Integer day, Long balance) {
        try {
            if (balance == null) {
                stringRedisTemplate.opsForHash().delete(buildStateKey(storeId, day), FIELD_BALANCE);
                return;
            }
            stringRedisTemplate.opsForHash().put(buildStateKey(storeId, day), FIELD_BALANCE, balance.toString());
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public Map<String, Boolean> getActions(Long storeId, int day) {
        String json = stringRedisTemplate.opsForValue().get(buildActionsKey(storeId, day));
        if (json == null) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
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
            stringRedisTemplate.opsForValue().set(buildActionsKey(storeId, day), objectMapper.writeValueAsString(actions));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize actions", e);
        }
    }

    String buildStateKey(Long storeId, Integer day) {
        return STATE_KEY_PATTERN.formatted(storeId, day);
    }

    String buildTickLogKey(Long storeId, Integer day) {
        return TICK_LOG_KEY_PATTERN.formatted(storeId, day);
    }

    String buildActionsKey(Long storeId, Integer day) {
        return ACTIONS_KEY_PATTERN.formatted(storeId, day);
    }

    private void saveTickLog(Long storeId, Integer day, GameDayLiveState state) {
        if (state.tick() == null || state.tick() <= 0) {
            return;
        }

        try {
            stringRedisTemplate.opsForHash().putAll(buildTickLogKey(storeId, day), buildTickLogEntries(state));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, String> buildEntries(GameDayLiveState state) {
        Map<String, String> entries = buildLiveEntries(state);
        put(entries, FIELD_STARTED_AT, state.startedAt());
        putJson(entries, FIELD_PURCHASE_LIST, state.purchaseList());
        put(entries, FIELD_PURCHASE_CURSOR, state.purchaseCursor());
        putJson(entries, FIELD_START_RESPONSE, state.startResponse());
        return entries;
    }

    private Map<String, String> buildLiveEntries(GameDayLiveState state) {
        Map<String, String> entries = new LinkedHashMap<>();
        put(entries, FIELD_PURCHASE_CURSOR, state.purchaseCursor());
        put(entries, FIELD_TICK, state.tick());
        put(entries, FIELD_POPULATION_PER_STORE, state.populationPerStore());
        put(entries, FIELD_CAPTURE_RATE, state.captureRate());
        put(entries, FIELD_SALE_PRICE, state.salePrice());
        put(entries, FIELD_TICK_CUSTOMER_COUNT, state.tickCustomerCount());
        put(entries, FIELD_TICK_PURCHASE_COUNT, state.tickPurchaseCount());
        put(entries, FIELD_TICK_SALES, state.tickSales());
        put(entries, FIELD_CUMULATIVE_CUSTOMER_COUNT, state.cumulativeCustomerCount());
        put(entries, FIELD_CUMULATIVE_PURCHASE_COUNT, state.cumulativePurchaseCount());
        put(entries, FIELD_CUMULATIVE_SALES, state.cumulativeSales());
        put(entries, FIELD_CUMULATIVE_TOTAL_COST, state.cumulativeTotalCost());
        put(entries, FIELD_BALANCE, state.balance());
        put(entries, FIELD_STOCK, state.stock());
        put(entries, FIELD_LAST_CALCULATED_AT, state.lastCalculatedAt());
        return entries;
    }

    private Map<String, String> buildTickLogEntries(GameDayLiveState state) {
        String prefix = "tick:%d:".formatted(state.tick());
        Map<String, String> entries = new LinkedHashMap<>();
        put(entries, prefix + "population_per_store", state.populationPerStore());
        put(entries, prefix + "capture_rate", state.captureRate());
        put(entries, prefix + "sale_price", state.salePrice());
        put(entries, prefix + "tick_customer_count", state.tickCustomerCount());
        put(entries, prefix + "tick_purchase_count", state.tickPurchaseCount());
        put(entries, prefix + "tick_sales", state.tickSales());
        put(entries, prefix + "cumulative_customer_count", state.cumulativeCustomerCount());
        put(entries, prefix + "cumulative_purchase_count", state.cumulativePurchaseCount());
        put(entries, prefix + "cumulative_sales", state.cumulativeSales());
        put(entries, prefix + "cumulative_total_cost", state.cumulativeTotalCost());
        put(entries, prefix + "balance", state.balance());
        put(entries, prefix + "stock", state.stock());
        return entries;
    }

    private void put(Map<String, String> entries, String field, Object value) {
        if (value == null) {
            return;
        }
        entries.put(field, value.toString());
    }

    private void putJson(Map<String, String> entries, String field, Object value) {
        if (value == null) {
            return;
        }
        try {
            entries.put(field, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Long parseLongObject(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.valueOf(text);
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

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return new BigDecimal(text);
    }

    private BigDecimal parseCaptureRate(Map<Object, Object> entries) {
        BigDecimal captureRate = parseBigDecimal(entries.get(FIELD_CAPTURE_RATE));
        if (captureRate != null) {
            return captureRate;
        }
        return parseBigDecimal(entries.get(LEGACY_FIELD_CAPTURE_RATE));
    }

    private List<Integer> parsePurchaseList(Object value) throws Exception {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Integer[] numbers = objectMapper.readValue(text, Integer[].class);
        return Arrays.asList(numbers);
    }

    private GameDayStartResponse parseStartResponse(Object value) throws Exception {
        if (value == null) {
            return null;
        }

        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return objectMapper.readValue(text, GameDayStartResponse.class);
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
