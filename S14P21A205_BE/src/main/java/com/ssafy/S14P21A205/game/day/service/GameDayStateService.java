package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
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
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final BigDecimal DECIMAL_ZERO = new BigDecimal("0.00");
    private static final Duration TICK_INTERVAL = Duration.ofSeconds(10);
    private static final SeasonTimeline SEASON_TIMELINE_POLICY = new SeasonTimeline();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyEventRepository dailyEventRepository;
    private final ActionLogRepository actionLogRepository;
    private final OrderRepository orderRepository;
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
        int tick = resolveCurrentTick(currentTimeline, effectiveNow);
        int purchaseCursor = resolvePurchaseCursorAtTick(state, currentTimeline, tick);
        ActionUsage actionUsage = resolveActionUsage(store.getId(), day);
        EmergencyOrderState emergencyOrderState = resolveEmergencyOrderState(store.getId(), day, effectiveNow);
        List<ResolvedEvent> resolvedEvents =
                resolveEvents(store.getSeason().getId(), day, totalDays, state.startedAt(), effectiveNow);

        CalculatedGameState calculatedState = calculateGameState(
                store,
                state,
                day,
                currentTimeline,
                tick,
                purchaseCursor,
                actionUsage,
                emergencyOrderState,
                resolvedEvents,
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

    private int resolvePurchaseCursor(
            GameDayLiveState state,
            SeasonTimeline.DayTimeline currentTimeline,
            LocalDateTime effectiveNow
    ) {
        List<Integer> purchaseList = state.purchaseList();
        if (purchaseList == null || purchaseList.isEmpty()) {
            return 0;
        }

        if (!effectiveNow.isAfter(currentTimeline.businessStart())) {
            return 0;
        }
        if (!effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return purchaseList.size();
        }

        long totalMillis = SEASON_TIMELINE_POLICY.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int computedCursor = (int) ((purchaseList.size() * boundedElapsedMillis) / totalMillis);
        int persistedCursor = state.purchaseCursor() == null ? 0 : state.purchaseCursor();
        return Math.min(purchaseList.size(), Math.max(persistedCursor, computedCursor));
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
            captureRateBoost = captureRateBoost.add(
                    actionLog.getAction().getCaptureRate() == null ? DECIMAL_ZERO : actionLog.getAction().getCaptureRate()
            );

            if (actionLog.getAction().getCategory() == ActionCategory.DISCOUNT) {
                discountUsed = true;
                continue;
            }
            if (actionLog.getAction().getCategory() == ActionCategory.DONATION) {
                donationUsed = true;
                continue;
            }
            if (actionLog.getAction().getCategory() != ActionCategory.PROMOTION) {
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

    private List<ResolvedEvent> resolveEvents(
            Long seasonId,
            int currentDay,
            int totalDays,
            LocalDateTime currentDayStart,
            LocalDateTime effectiveNow
    ) {
        List<DailyEvent> dailyEvents = dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                seasonId,
                1,
                currentDay
        );

        List<ResolvedEvent> resolvedEvents = new ArrayList<>();
        for (DailyEvent dailyEvent : dailyEvents) {
            int appliedDay = resolveAppliedDay(dailyEvent);
            if (appliedDay < 1 || appliedDay > currentDay) {
                continue;
            }

            LocalDateTime appliedAt = SEASON_TIMELINE_POLICY.resolveAppliedAt(
                    currentDayStart,
                    currentDay,
                    totalDays,
                    appliedDay,
                    dailyEvent.getApplyOffsetSeconds()
            );
            if (appliedAt.isAfter(effectiveNow)) {
                continue;
            }

            LocalDateTime endedAt = SEASON_TIMELINE_POLICY.resolveEndedAt(
                    currentDayStart,
                    currentDay,
                    totalDays,
                    appliedDay,
                    dailyEvent.getExpireOffsetSeconds(),
                    dailyEvent.getEvent().getEndTime()
            );
            resolvedEvents.add(new ResolvedEvent(dailyEvent, appliedDay, appliedAt, endedAt));
        }
        return resolvedEvents;
    }

    private int resolveAppliedDay(DailyEvent dailyEvent) {
        return dailyEvent.getEvent().getStartTime() == EventStartTime.NEXT_DAY
                ? dailyEvent.getDay() + 1
                : dailyEvent.getDay();
    }

    private CalculatedGameState calculateGameState(
            Store store,
            GameDayLiveState state,
            int day,
            SeasonTimeline.DayTimeline currentTimeline,
            int tick,
            int purchaseCursor,
            ActionUsage actionUsage,
            EmergencyOrderState emergencyOrderState,
            List<ResolvedEvent> resolvedEvents,
            LocalDateTime effectiveNow,
            Order dailyStartOrder
    ) {
        long demandUnits = calculateDemandUnits(state.purchaseList(), purchaseCursor);
        long capitalChange = 0L;
        int stockChange = emergencyOrderState.arrivedStock();
        BigDecimal populationEventMultiplier = DECIMAL_ONE;

        List<GameStateResponse.AppliedEvent> responseAppliedEvents = new ArrayList<>();
        for (ResolvedEvent resolvedEvent : resolvedEvents) {
            RandomEvent event = resolvedEvent.dailyEvent().getEvent();

            if (resolvedEvent.appliedDay() == day) {
                capitalChange += event.getCapitalFlat() == null ? 0L : event.getCapitalFlat();
                stockChange += toWholeNumber(event.getStockFlat());
            }

            if (resolvedEvent.isActiveAt(effectiveNow)) {
                populationEventMultiplier = populationEventMultiplier.multiply(normalizeRate(event.getPopulationRate()));
                responseAppliedEvents.add(new GameStateResponse.AppliedEvent(
                        event.getEventType(),
                        event.getEventType(),
                        resolvedEvent.dailyEvent().getNewsTitle(),
                        resolvedEvent.appliedAt()
                ));
            }
        }

        int totalAvailableStock = Math.max(0, state.startResponse().initialStock() + stockChange);
        long actualSoldUnits = Math.min(demandUnits, totalAvailableStock);
        int remainingStock = (int) Math.max(0L, totalAvailableStock - actualSoldUnits);
        long cumulativeSales = Math.multiplyExact(actualSoldUnits, state.salePrice().longValue());
        long cumulativeTotalCost = valueOf(store.getLocation() == null ? null : store.getLocation().getRent())
                + valueOf(dailyStartOrder == null ? null : dailyStartOrder.getTotalCost())
                + actionUsage.totalCost()
                + emergencyOrderState.totalCost();
        long cash = state.startResponse().initialBalance()
                + cumulativeSales
                + capitalChange
                - actionUsage.totalCost()
                - emergencyOrderState.totalCost();

        int population = calculateCurrentPopulation(state.startResponse(), currentTimeline, populationEventMultiplier, effectiveNow);
        TickProgress tickProgress = calculateTickProgress(state, currentTimeline, tick, totalAvailableStock);
        return new CalculatedGameState(
                cash,
                remainingStock,
                population,
                responseAppliedEvents,
                new GameDayLiveState(
                        state.startedAt(),
                        state.purchaseList(),
                        purchaseCursor,
                        state.startResponse(),
                        tick,
                        population,
                        resolveInflowRate(state.startResponse().captureRate(), actionUsage.captureRateBoost()),
                        state.salePrice(),
                        tickProgress.tickCustomerCount(),
                        tickProgress.tickPurchaseCount(),
                        tickProgress.tickSales(),
                        tickProgress.cumulativeCustomerCount(),
                        safeToInt(actualSoldUnits),
                        cumulativeSales,
                        cumulativeTotalCost,
                        cash,
                        remainingStock,
                        effectiveNow
                )
        );
    }

    private long calculateDemandUnits(List<Integer> purchaseList, int purchaseCursor) {
        if (purchaseList == null || purchaseList.isEmpty() || purchaseCursor <= 0) {
            return 0L;
        }

        long demandUnits = 0L;
        int cursor = Math.min(purchaseCursor, purchaseList.size());
        for (int index = 0; index < cursor; index++) {
            demandUnits += purchaseList.get(index);
        }
        return demandUnits;
    }

    private int calculateCurrentPopulation(
            GameDayStartResponse startResponse,
            SeasonTimeline.DayTimeline currentTimeline,
            BigDecimal populationEventMultiplier,
            LocalDateTime effectiveNow
    ) {
        if (!effectiveNow.isAfter(currentTimeline.businessStart()) || !effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return 0;
        }

        if (startResponse.hourlySchedule() == null || startResponse.hourlySchedule().isEmpty()) {
            return 0;
        }

        List<GameDayStartResponse.HourlySchedule> schedules = new ArrayList<>(startResponse.hourlySchedule().values());
        long totalMillis = SEASON_TIMELINE_POLICY.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int scheduleIndex = (int) Math.min(
                schedules.size() - 1L,
                (boundedElapsedMillis * schedules.size()) / totalMillis
        );
        GameDayStartResponse.HourlySchedule schedule = schedules.get(scheduleIndex);

        BigDecimal population = BigDecimal.valueOf(schedule.population())
                .multiply(normalizeRate(startResponse.weatherMultiplier()))
                .multiply(normalizeRate(schedule.trafficMultiplier()))
                .multiply(populationEventMultiplier);
        return population.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private int toWholeNumber(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return DECIMAL_ONE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveInflowRate(BigDecimal baseCaptureRate, BigDecimal actionCaptureRateBoost) {
        BigDecimal resolved = baseCaptureRate == null ? DECIMAL_ZERO : baseCaptureRate;
        if (actionCaptureRateBoost != null) {
            resolved = resolved.add(actionCaptureRateBoost);
        }
        return resolved.max(DECIMAL_ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private TickProgress calculateTickProgress(
            GameDayLiveState state,
            SeasonTimeline.DayTimeline currentTimeline,
            int tick,
            int totalAvailableStock
    ) {
        if (tick <= 0) {
            return new TickProgress(0, 0, 0, 0L);
        }

        int previousCursor = resolvePurchaseCursorAtTick(state, currentTimeline, tick - 1);
        int currentCursor = resolvePurchaseCursorAtTick(state, currentTimeline, tick);
        long previousDemandUnits = calculateDemandUnits(state.purchaseList(), previousCursor);
        long currentDemandUnits = calculateDemandUnits(state.purchaseList(), currentCursor);
        int previousSoldUnits = safeToInt(Math.min(previousDemandUnits, totalAvailableStock));
        int currentSoldUnits = safeToInt(Math.min(currentDemandUnits, totalAvailableStock));
        int tickPurchaseCount = Math.max(0, currentSoldUnits - previousSoldUnits);

        return new TickProgress(
                Math.max(0, currentCursor - previousCursor),
                tickPurchaseCount,
                currentCursor,
                Math.multiplyExact((long) tickPurchaseCount, state.salePrice().longValue())
        );
    }

    private int resolveCurrentTick(SeasonTimeline.DayTimeline currentTimeline, LocalDateTime effectiveNow) {
        if (!effectiveNow.isAfter(currentTimeline.businessStart())) {
            return 0;
        }
        if (!effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return totalTickCount();
        }

        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        return (int) Math.min(totalTickCount(), Math.max(0L, elapsedMillis / TICK_INTERVAL.toMillis()));
    }

    private int resolvePurchaseCursorAtTick(
            GameDayLiveState state,
            SeasonTimeline.DayTimeline currentTimeline,
            int tick
    ) {
        if (tick <= 0) {
            return 0;
        }

        LocalDateTime tickBoundary = currentTimeline.businessStart().plus(TICK_INTERVAL.multipliedBy(tick));
        if (tickBoundary.isAfter(currentTimeline.businessEnd())) {
            tickBoundary = currentTimeline.businessEnd();
        }
        return resolvePurchaseCursor(state, currentTimeline, tickBoundary);
    }

    private int totalTickCount() {
        return (int) (SEASON_TIMELINE_POLICY.businessDuration().toMillis() / TICK_INTERVAL.toMillis());
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

    private record ResolvedEvent(
            DailyEvent dailyEvent,
            int appliedDay,
            LocalDateTime appliedAt,
            LocalDateTime endedAt
    ) {
        private boolean isActiveAt(LocalDateTime now) {
            return endedAt == null || now.isBefore(endedAt);
        }
    }

    private record TickProgress(
            int tickCustomerCount,
            int tickPurchaseCount,
            int cumulativeCustomerCount,
            long tickSales
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
