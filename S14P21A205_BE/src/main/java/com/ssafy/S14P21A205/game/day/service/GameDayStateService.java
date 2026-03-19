package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.engine.StockEngine;
import com.ssafy.S14P21A205.game.day.policy.CaptureRatePolicy;
import com.ssafy.S14P21A205.game.day.policy.CostPolicy;
import com.ssafy.S14P21A205.game.day.policy.PopulationPolicy;
import com.ssafy.S14P21A205.game.day.resolver.EventEffectResolver;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.model.SeasonTimePoint;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.entity.OrderType;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStateService {

    private static final SeasonTimelineService SEASON_TIMELINE_SERVICE = new SeasonTimelineService();

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
    private final Clock clock;

    @Transactional
    public GameStateResponse getGameState(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        Store store = getActiveStore(user.getId());
        return refreshGameState(store)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Optional<GameStateResponse> refreshGameState(Store store) {
        LocalDateTime serverTime = LocalDateTime.now(clock);
        SeasonTimePoint seasonTimePoint = SEASON_TIMELINE_SERVICE.resolve(store.getSeason(), serverTime);
        if (!seasonTimePoint.isPlayableDayPhase()) {
            log.debug(
                    "state-skip storeId={} seasonId={} now={} phase={}",
                    store.getId(),
                    store.getSeason().getId(),
                    serverTime,
                    seasonTimePoint.phase()
            );
            return Optional.empty();
        }
        int day = resolveCurrentDay(store.getSeason(), seasonTimePoint);
        int totalDays = store.getSeason().getTotalDays();

        GameDayLiveState rawState = gameDayStoreStateRedisRepository.find(store.getId(), day)
                .orElse(null);
        if (rawState == null || rawState.startResponse() == null) {
            return Optional.empty();
        }

        Order dailyStartOrder = orderRepository.findDailyStartOrder(store.getId(), day).orElse(null);
        GameDayLiveState state = normalizeState(rawState, dailyStartOrder);
        DayWindow currentTimeline = SEASON_TIMELINE_SERVICE.day(store.getSeason(), day);

        LocalDateTime effectiveNow = min(serverTime, currentTimeline.reportEnd());
        int tick = stockEngine.resolveCurrentTick(currentTimeline, effectiveNow);
        log.info(
                "state-timeline storeId={} seasonId={} now={} phase={} day={} gameTime={} tick={} effectiveNow={}",
                store.getId(),
                store.getSeason().getId(),
                serverTime,
                seasonTimePoint.phase(),
                day,
                seasonTimePoint.gameTime(),
                tick,
                effectiveNow
        );
        ActionUsage actionUsage = resolveActionUsage(store.getId(), day);
        long regionStoreCount = resolveRegionStoreCount(store);

        CalculatedGameState calculatedState = calculateGameState(
                store,
                state,
                currentTimeline,
                tick,
                actionUsage,
                effectiveNow,
                dailyStartOrder,
                day,
                totalDays,
                regionStoreCount
        );

        gameDayStoreStateRedisRepository.saveStateAndTickLog(store.getId(), day, calculatedState.liveState());
        log.info(
                "state-updated storeId={} seasonId={} day={} tick={} populationPerStore={} cash={} stock={}",
                store.getId(),
                store.getSeason().getId(),
                day,
                tick,
                calculatedState.populationPerStore(),
                calculatedState.cash(),
                calculatedState.totalStock()
        );

        return Optional.of(new GameStateResponse(
                serverTime,
                store.getSeason().getId(),
                day,
                String.valueOf(calculatedState.populationPerStore()),
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
                        calculatedState.emergencyOrderState().pending(),
                        calculatedState.emergencyOrderState().pending() ? calculatedState.emergencyOrderState().arriveAt() : null
                ),
                calculatedState.appliedEvents()
        ));
    }

    private Store getActiveStore(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private int resolveCurrentDay(Season season, SeasonTimePoint seasonTimePoint) {
        Integer currentDay = seasonTimePoint.currentDay();
        if (currentDay == null || currentDay < 1 || currentDay > season.getTotalDays()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "Current season day is out of range.");
        }
        return currentDay;
    }

    private long resolveRegionStoreCount(Store store) {
        long count = storeRepository.countBySeason_IdAndLocation_Id(store.getSeason().getId(), store.getLocation().getId());
        return Math.max(1L, count);
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
                state.tick() == null ? 0 : state.tick(),
                state.populationPerStore() == null ? 0 : state.populationPerStore(),
                state.captureRate(),
                state.salePrice() == null ? 0 : state.salePrice(),
                state.tickCustomerCount() == null ? 0 : state.tickCustomerCount(),
                state.tickPurchaseCount() == null ? 0 : state.tickPurchaseCount(),
                state.tickSales() == null ? 0L : state.tickSales(),
                state.cumulativeCustomerCount() == null ? 0 : state.cumulativeCustomerCount(),
                state.cumulativePurchaseCount() == null ? 0 : state.cumulativePurchaseCount(),
                state.cumulativeSales() == null ? 0L : state.cumulativeSales(),
                state.cumulativeTotalCost() == null ? 0L : state.cumulativeTotalCost(),
                state.balance() == null ? 0L : state.balance(),
                state.stock() == null ? initialStockOf(state) : state.stock(),
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

        for (ActionLog actionLog : actionLogs) {
            if (actionLog.getAction() == null) {
                continue;
            }

            totalCost += actionLog.getAction().getCost() == null ? 0L : actionLog.getAction().getCost();

            ActionCategory category = actionLog.getAction().getCategory();
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
                totalCost
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
            DayWindow currentTimeline,
            int tick,
            ActionUsage actionUsage,
            LocalDateTime effectiveNow,
            Order dailyStartOrder,
            int day,
            int totalDays,
            long regionStoreCount
    ) {
        BigDecimal captureRate = resolveLiveCaptureRate(state);
        ProgressionState progressionState = progressStateByTick(
                store,
                state,
                currentTimeline,
                tick,
                effectiveNow,
                captureRate,
                day,
                totalDays,
                regionStoreCount
        );
        EventEffectResolver.EventEffect eventEffect = progressionState.currentEventEffect();
        EmergencyOrderState emergencyOrderState = progressionState.currentEmergencyOrderState();
        CostPolicy.CostResult costResult = costPolicy.calculate(
                store,
                dailyStartOrder,
                state.startResponse(),
                actionUsage.totalCost(),
                emergencyOrderState.totalCost(),
                eventEffect.capitalChange(),
                progressionState.cumulativeSales(),
                state.startResponse().initialBalance()
        );

        return new CalculatedGameState(
                costResult.cash(),
                progressionState.stock(),
                progressionState.populationPerStore(),
                emergencyOrderState,
                eventEffect.appliedEvents(),
                new GameDayLiveState(
                        state.startedAt(),
                        state.purchaseList(),
                        progressionState.purchaseCursor(),
                        state.startResponse(),
                        tick,
                        progressionState.populationPerStore(),
                        captureRate,
                        state.salePrice(),
                        progressionState.tickCustomerCount(),
                        progressionState.tickPurchaseCount(),
                        progressionState.tickSales(),
                        progressionState.cumulativeCustomerCount(),
                        progressionState.cumulativePurchaseCount(),
                        progressionState.cumulativeSales(),
                        costResult.cumulativeTotalCost(),
                        costResult.cash(),
                        progressionState.stock(),
                        effectiveNow
                )
        );
    }

    private ProgressionState progressStateByTick(
            Store store,
            GameDayLiveState state,
            DayWindow currentTimeline,
            int currentTick,
            LocalDateTime effectiveNow,
            BigDecimal captureRate,
            int day,
            int totalDays,
            long regionStoreCount
    ) {
        int processedTick = state.tick() == null ? 0 : state.tick();
        int purchaseCursor = state.purchaseCursor() == null ? 0 : state.purchaseCursor();
        int tickCustomerCount = state.tickCustomerCount() == null ? 0 : state.tickCustomerCount();
        int tickPurchaseCount = state.tickPurchaseCount() == null ? 0 : state.tickPurchaseCount();
        long tickSales = state.tickSales() == null ? 0L : state.tickSales();
        int cumulativeCustomerCount = state.cumulativeCustomerCount() == null ? 0 : state.cumulativeCustomerCount();
        int cumulativePurchaseCount = state.cumulativePurchaseCount() == null ? 0 : state.cumulativePurchaseCount();
        long cumulativeSales = state.cumulativeSales() == null ? 0L : state.cumulativeSales();
        int stock = state.stock() == null ? initialStockOf(state) : state.stock();

        LocalDateTime baselineTime = state.lastCalculatedAt();
        EventEffectResolver.EventEffect baselineEffect = resolveEventEffect(store, day, baselineTime);
        EmergencyOrderState baselineEmergency = resolveEmergencyOrderState(store.getId(), day, baselineTime);

        for (int nextTick = processedTick + 1; nextTick <= currentTick; nextTick++) {
            LocalDateTime tickBoundary = stockEngine.resolveTickBoundary(currentTimeline, nextTick);
            EventEffectResolver.EventEffect tickEffect = resolveEventEffect(store, day, tickBoundary);
            EmergencyOrderState tickEmergency = resolveEmergencyOrderState(store.getId(), day, tickBoundary);

            int populationPerStore = resolvePopulationPerStore(
                    state,
                    currentTimeline,
                    tickEffect.populationEventMultiplier(),
                    tickBoundary,
                    regionStoreCount
            );
            int desiredCustomerCount = stockEngine.calculateTickCustomerCount(populationPerStore, captureRate);
            int nextCursor = stockEngine.advancePurchaseCursor(state.purchaseList(), purchaseCursor, desiredCustomerCount);
            int actualCustomerCount = Math.max(0, nextCursor - purchaseCursor);
            long demandUnits = stockEngine.calculateDemandUnits(state.purchaseList(), purchaseCursor, nextCursor);
            int availableStock = Math.max(
                    0,
                    stock
                            + applyStockEventDelta(stock, baselineEffect, tickEffect)
                            + deltaArrivedStock(baselineEmergency, tickEmergency)
            );
            int soldUnits = safeToInt(Math.min(demandUnits, availableStock));

            tickCustomerCount = actualCustomerCount;
            tickPurchaseCount = soldUnits;
            tickSales = Math.multiplyExact((long) soldUnits, valueOf(state.salePrice()));
            cumulativeCustomerCount += actualCustomerCount;
            cumulativePurchaseCount += soldUnits;
            cumulativeSales += tickSales;
            stock = availableStock - soldUnits;
            purchaseCursor = nextCursor;
            baselineEffect = tickEffect;
            baselineEmergency = tickEmergency;
        }

        EventEffectResolver.EventEffect currentEffect = resolveEventEffect(store, day, effectiveNow);
        EmergencyOrderState currentEmergency = resolveEmergencyOrderState(store.getId(), day, effectiveNow);
        int stockNow = Math.max(
                0,
                stock
                        + applyStockEventDelta(stock, baselineEffect, currentEffect)
                        + deltaArrivedStock(baselineEmergency, currentEmergency)
        );
        int currentPopulationPerStore = resolvePopulationPerStore(
                state,
                currentTimeline,
                currentEffect.populationEventMultiplier(),
                effectiveNow,
                regionStoreCount
        );

        return new ProgressionState(
                purchaseCursor,
                tickCustomerCount,
                tickPurchaseCount,
                tickSales,
                cumulativeCustomerCount,
                cumulativePurchaseCount,
                cumulativeSales,
                stockNow,
                currentPopulationPerStore,
                currentEffect,
                currentEmergency
        );
    }

    private EventEffectResolver.EventEffect resolveEventEffect(Store store, int day, LocalDateTime effectiveNow) {
        return eventEffectResolver.resolve(
                store.getSeason(),
                day,
                effectiveNow,
                store.getLocation().getId(),
                store.getMenu().getId()
        );
    }

    private BigDecimal resolveLiveCaptureRate(GameDayLiveState state) {
        if (state.captureRate() != null) {
            return captureRatePolicy.normalizeCaptureRate(state.captureRate());
        }
        if (state.startResponse() != null && state.startResponse().captureRate() != null) {
            return captureRatePolicy.normalizeCaptureRate(state.startResponse().captureRate());
        }
        return captureRatePolicy.normalizeCaptureRate(BigDecimal.ZERO);
    }

    private int resolvePopulationPerStore(
            GameDayLiveState state,
            DayWindow currentTimeline,
            BigDecimal populationEventMultiplier,
            LocalDateTime effectiveNow,
            long regionStoreCount
    ) {
        int totalPopulation = populationPolicy.calculateCurrentPopulation(
                state.startResponse(),
                currentTimeline,
                populationEventMultiplier,
                effectiveNow
        );
        if (totalPopulation <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(totalPopulation)
                .divide(BigDecimal.valueOf(Math.max(1L, regionStoreCount)), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int applyStockEventDelta(
            int currentStock,
            EventEffectResolver.EventEffect previous,
            EventEffectResolver.EventEffect current
    ) {
        int adjustedStock = currentStock + (current.stockChange() - previous.stockChange());
        Set<Long> previouslyAppliedEventIds = new HashSet<>();
        for (EventEffectResolver.StockRateEvent stockRateEvent : previous.appliedStockRateEvents()) {
            if (stockRateEvent.dailyEventId() != null) {
                previouslyAppliedEventIds.add(stockRateEvent.dailyEventId());
            }
        }

        for (EventEffectResolver.StockRateEvent stockRateEvent : current.appliedStockRateEvents()) {
            Long dailyEventId = stockRateEvent.dailyEventId();
            if (dailyEventId != null && previouslyAppliedEventIds.contains(dailyEventId)) {
                continue;
            }
            adjustedStock = BigDecimal.valueOf(adjustedStock)
                    .multiply(stockRateEvent.stockRate())
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return adjustedStock - currentStock;
    }

    private int deltaArrivedStock(EmergencyOrderState previous, EmergencyOrderState current) {
        return current.arrivedStock() - previous.arrivedStock();
    }

    private int initialStockOf(GameDayLiveState state) {
        return state.startResponse() == null || state.startResponse().initialStock() == null
                ? 0
                : state.startResponse().initialStock();
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
            long totalCost
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
            int populationPerStore,
            EmergencyOrderState emergencyOrderState,
            List<GameStateResponse.AppliedEvent> appliedEvents,
            GameDayLiveState liveState
    ) {
    }

    private record ProgressionState(
            int purchaseCursor,
            int tickCustomerCount,
            int tickPurchaseCount,
            long tickSales,
            int cumulativeCustomerCount,
            int cumulativePurchaseCount,
            long cumulativeSales,
            int stock,
            int populationPerStore,
            EventEffectResolver.EventEffect currentEventEffect,
            EmergencyOrderState currentEmergencyOrderState
    ) {
    }
}

