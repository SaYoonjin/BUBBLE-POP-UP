package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.policy.CostPolicy;
import com.ssafy.S14P21A205.game.day.resolver.EventEffectResolver;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.day.engine.StockEngine;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.entity.OrderType;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStateService {

    private static final BigDecimal DECIMAL_ZERO = new BigDecimal("0.00");
    private static final SeasonTimeline SEASON_TIMELINE_POLICY = new SeasonTimeline();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final ActionLogRepository actionLogRepository;
    private final OrderRepository orderRepository;
    private final EventEffectResolver eventEffectResolver;
    private final StockEngine stockEngine;
    private final PopulationPolicy populationPolicy;
    private final CaptureRatePolicy captureRatePolicy;
    private final CostPolicy costPolicy;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public GameStateResponse getGameState(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        Store store = getActiveStore(user.getId());
        return refreshGameState(store)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Optional<GameStateResponse> refreshGameState(Store store) {
        int day = resolveCurrentDay(store.getSeason());
        int totalDays = store.getSeason().getTotalDays();

        GameDayLiveState rawState = gameDayStoreStateRedisRepository.find(store.getId(), day)
                .orElse(null);
        if (rawState == null || rawState.startResponse() == null) {
            return Optional.empty();
        }

        Order dailyStartOrder = orderRepository.findDailyStartOrder(store.getId(), day).orElse(null);
        GameDayLiveState state = normalizeState(rawState, dailyStartOrder);
        SeasonTimeline.DayTimeline currentTimeline =
                SEASON_TIMELINE_POLICY.currentDay(state.startedAt(), day, totalDays);

        LocalDateTime serverTime = LocalDateTime.now(clock);
        LocalDateTime effectiveNow = min(serverTime, currentTimeline.reportEnd());
        int tick = stockEngine.resolveCurrentTick(currentTimeline, effectiveNow);
        int purchaseCursor = stockEngine.resolvePurchaseCursorAtTick(state, currentTimeline, tick);
        ActionUsage actionUsage = resolveActionUsage(store.getId(), day);
        EmergencyOrderState emergencyOrderState = resolveEmergencyOrderState(store.getId(), day, effectiveNow);
        EventEffectResolver.EventEffect eventEffect = eventEffectResolver.resolve(
                store.getSeason().getId(),
                day,
                totalDays,
                state.startedAt(),
                effectiveNow
        );

        CalculatedGameState calculatedState = calculateGameState(
                store,
                state,
                currentTimeline,
                tick,
                purchaseCursor,
                actionUsage,
                emergencyOrderState,
                eventEffect,
                effectiveNow,
                dailyStartOrder
        );

        gameDayStoreStateRedisRepository.saveStateAndTickLog(store.getId(), day, calculatedState.liveState());

        return Optional.of(new GameStateResponse(
                serverTime,
                store.getSeason().getId(),
                day,
                String.valueOf(calculatedState.population()),
                effectiveNow,
                calculatedState.cash(),
                calculatedState.liveState().cumulativeCustomerCount(),
                new GameStateResponse.Inventory(calculatedState.totalStock()),
                new GameStateResponse.ActionStatus(
                        actionUsage.discountUsed(),
                        actionUsage.donationUsed(),
                        actionUsage.influencerUsed(),
                        actionUsage.snsUsed(),
                        actionUsage.leafletUsed(),
                        actionUsage.friendUsed(),
                        emergencyOrderState.pending(),
                        emergencyOrderState.pending() ? emergencyOrderState.arriveAt() : null
                ),
                calculatedState.appliedEvents()
        ));
    }

    private Store getActiveStore(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private int resolveCurrentDay(Season season) {
        int currentDay = season.getCurrentDay() == null ? 1 : season.getCurrentDay();
        if (currentDay < 1 || currentDay > season.getTotalDays()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "Current season day is out of range.");
        }
        return currentDay;
    }

    private GameDayLiveState normalizeState(GameDayLiveState state, Order dailyStartOrder) {
        LocalDateTime startedAt = state.startedAt();
        if (startedAt == null) {
            startedAt = dailyStartOrder != null && dailyStartOrder.getCreatedAt() != null
                    ? dailyStartOrder.getCreatedAt()
                    : LocalDateTime.now(clock);
        }

        LocalDateTime lastCalculatedAt = state.lastCalculatedAt();
        if (lastCalculatedAt == null || lastCalculatedAt.isBefore(startedAt)) {
            lastCalculatedAt = startedAt;
        }

        int purchaseCursor = state.purchaseCursor() == null ? 0 : state.purchaseCursor();
        return new GameDayLiveState(
                startedAt,
                state.purchaseList(),
                purchaseCursor,
                state.startResponse(),
                state.tick(),
                state.populationPerStore(),
                state.inflowRate(),
                state.salePrice(),
                state.tickCustomerCount(),
                state.tickPurchaseCount(),
                state.tickSales(),
                state.cumulativeCustomerCount(),
                state.cumulativePurchaseCount(),
                state.cumulativeSales(),
                state.cumulativeTotalCost(),
                state.balance(),
                state.stock(),
                lastCalculatedAt
        );
    }

    private ActionUsage resolveActionUsage(Long storeId, int day) {
        List<ActionLog> actionLogs = actionLogRepository.findByStore_IdAndGameDayAndIsUsedTrue(storeId, day);

        boolean discountUsed = false;
        boolean donationUsed = false;
        boolean influencerUsed = false;
        boolean snsUsed = false;
        boolean leafletUsed = false;
        boolean friendUsed = false;
        long totalCost = 0L;
        BigDecimal captureRateBoost = DECIMAL_ZERO;

        for (ActionLog actionLog : actionLogs) {
            if (actionLog.getAction() == null) {
                continue;
            }

            totalCost += actionLog.getAction().getCost() == null ? 0L : actionLog.getAction().getCost();

            // 할인/나눔은 actionValue(동적 값)를 사용, 그 외는 Action 테이블의 고정 captureRate 사용
            ActionCategory category = actionLog.getAction().getCategory();
            if (category == ActionCategory.DISCOUNT || category == ActionCategory.DONATION) {
                BigDecimal dynamicValue = actionLog.getActionValue() == null ? DECIMAL_ZERO : actionLog.getActionValue();
                captureRateBoost = captureRateBoost.add(dynamicValue);
            } else {
                captureRateBoost = captureRateBoost.add(
                        actionLog.getAction().getCaptureRate() == null ? DECIMAL_ZERO : actionLog.getAction().getCaptureRate()
                );
            }

            if (category == ActionCategory.DISCOUNT) {
                discountUsed = true;
                continue;
            }
            if (category == ActionCategory.DONATION) {
                donationUsed = true;
                continue;
            }
            if (category != ActionCategory.PROMOTION) {
                continue;
            }

            PromotionType promotionType = actionLog.getAction().getPromotionType();
            if (promotionType == PromotionType.INFLUENCER) {
                influencerUsed = true;
            } else if (promotionType == PromotionType.SNS) {
                snsUsed = true;
            } else if (promotionType == PromotionType.LEAFLET) {
                leafletUsed = true;
            } else if (promotionType == PromotionType.FRIEND) {
                friendUsed = true;
            }
        }

        return new ActionUsage(
                discountUsed,
                donationUsed,
                influencerUsed,
                snsUsed,
                leafletUsed,
                friendUsed,
                totalCost,
                captureRateBoost
        );
    }

    private EmergencyOrderState resolveEmergencyOrderState(Long storeId, int day, LocalDateTime effectiveNow) {
        List<Order> emergencyOrders = orderRepository.findByStoreIdAndOrderedDayAndOrderTypeOrderByArrivedTimeAscIdAsc(
                storeId,
                day,
                OrderType.EMERGENCY
        );

        int arrivedStock = 0;
        LocalDateTime pendingArriveAt = null;
        long totalCost = 0L;
        for (Order emergencyOrder : emergencyOrders) {
            totalCost += valueOf(emergencyOrder.getTotalCost());
            LocalDateTime arrivedTime = emergencyOrder.getArrivedTime();
            boolean arrived = Boolean.TRUE.equals(emergencyOrder.getIsArrived())
                    || (arrivedTime != null && !arrivedTime.isAfter(effectiveNow));
            if (arrived) {
                arrivedStock += emergencyOrder.getQuantity();
                if (!Boolean.TRUE.equals(emergencyOrder.getIsArrived())) {
                    emergencyOrder.markArrived();
                }
                continue;
            }

            if (pendingArriveAt == null
                    || (arrivedTime != null && pendingArriveAt != null && arrivedTime.isBefore(pendingArriveAt))
                    || (arrivedTime != null && pendingArriveAt == null)) {
                pendingArriveAt = arrivedTime;
            }
        }

        return new EmergencyOrderState(pendingArriveAt != null, pendingArriveAt, arrivedStock, totalCost);
    }

    private CalculatedGameState calculateGameState(
            Store store,
            GameDayLiveState state,
            SeasonTimeline.DayTimeline currentTimeline,
            int tick,
            int purchaseCursor,
            ActionUsage actionUsage,
            EmergencyOrderState emergencyOrderState,
            EventEffectResolver.EventEffect eventEffect,
            LocalDateTime effectiveNow,
            Order dailyStartOrder
    ) {
        long demandUnits = stockEngine.calculateDemandUnits(state.purchaseList(), purchaseCursor);
        StockEngine.StockCalculation stockCalculation = stockEngine.calculateStock(
                state,
                eventEffect.stockChange(),
                emergencyOrderState.arrivedStock(),
                demandUnits
        );
        CostPolicy.CostResult costResult = costPolicy.calculate(
                store,
                dailyStartOrder,
                actionUsage.totalCost(),
                emergencyOrderState.totalCost(),
                eventEffect.capitalChange(),
                stockCalculation.actualSoldUnits(),
                state.salePrice(),
                state.startResponse().initialBalance()
        );
        int population = populationPolicy.calculateCurrentPopulation(
                state.startResponse(),
                currentTimeline,
                eventEffect.populationEventMultiplier(),
                effectiveNow
        );
        StockEngine.TickProgress tickProgress = stockEngine.calculateTickProgress(
                state,
                currentTimeline,
                tick,
                stockCalculation.totalAvailableStock()
        );
        BigDecimal inflowRate = state.inflowRate() != null
                ? state.inflowRate()
                : captureRatePolicy.resolveInflowRate(state.startResponse().captureRate(), actionUsage.captureRateBoost());

        return new CalculatedGameState(
                costResult.cash(),
                stockCalculation.remainingStock(),
                population,
                eventEffect.appliedEvents(),
                new GameDayLiveState(
                        state.startedAt(),
                        state.purchaseList(),
                        purchaseCursor,
                        state.startResponse(),
                        tick,
                        population,
                        inflowRate,
                        state.salePrice(),
                        tickProgress.tickCustomerCount(),
                        tickProgress.tickPurchaseCount(),
                        tickProgress.tickSales(),
                        tickProgress.cumulativeCustomerCount(),
                        safeToInt(stockCalculation.actualSoldUnits()),
                        costResult.cumulativeSales(),
                        costResult.cumulativeTotalCost(),
                        costResult.cash(),
                        stockCalculation.remainingStock(),
                        effectiveNow
                )
        );
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private int safeToInt(long value) {
        return Math.toIntExact(Math.max(0L, value));
    }

    private LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private record ActionUsage(
            boolean discountUsed,
            boolean donationUsed,
            boolean influencerUsed,
            boolean snsUsed,
            boolean leafletUsed,
            boolean friendUsed,
            long totalCost,
            BigDecimal captureRateBoost
    ) {
    }

    private record EmergencyOrderState(
            boolean pending,
            LocalDateTime arriveAt,
            int arrivedStock,
            long totalCost
    ) {
    }

    private record CalculatedGameState(
            long cash,
            int totalStock,
            int population,
            List<GameStateResponse.AppliedEvent> appliedEvents,
            GameDayLiveState liveState
    ) {
    }
}
