package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import com.ssafy.S14P21A205.action.repository.ActionLogRepository;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDaySnapshot;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.repository.GameDaySnapshotRedisRepository;
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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDayStateService {

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final SeasonTimeline SEASON_TIMELINE_POLICY = new SeasonTimeline();

    private final UserService userService;
    private final StoreRepository storeRepository;
    private final DailyEventRepository dailyEventRepository;
    private final ActionLogRepository actionLogRepository;
    private final OrderRepository orderRepository;
    private final GameDaySnapshotRedisRepository gameDaySnapshotRedisRepository;

    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public GameStateResponse getGameState(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        Store store = getActiveStore(user.getId());
        int day = resolveCurrentDay(store.getSeason());
        int totalDays = store.getSeason().getTotalDays();

        GameDaySnapshot rawSnapshot = gameDaySnapshotRedisRepository.find(user.getId(), store.getSeason().getId(), day)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
        Order dailyStartOrder = orderRepository.findDailyStartOrder(store.getId(), day).orElse(null);
        GameDaySnapshot snapshot = normalizeSnapshot(rawSnapshot, dailyStartOrder);

        SeasonTimeline.DayTimeline currentTimeline =
                SEASON_TIMELINE_POLICY.currentDay(snapshot.startedAt(), day, totalDays);

        LocalDateTime serverTime = LocalDateTime.now(clock);
        LocalDateTime effectiveNow = min(serverTime, currentTimeline.reportEnd());
        LocalDateTime responseLastCalculatedAt = snapshot.lastCalculatedAt();

        int purchaseCursor = resolvePurchaseCursor(snapshot, currentTimeline, effectiveNow);
        ActionUsage actionUsage = resolveActionUsage(store.getId(), day);
        EmergencyOrderState emergencyOrderState = resolveEmergencyOrderState(store.getId(), day, effectiveNow);
        List<ResolvedEvent> resolvedEvents =
                resolveEvents(store.getSeason().getId(), day, totalDays, snapshot.startedAt(), effectiveNow);

        CalculatedGameState calculatedState = calculateGameState(
                snapshot,
                currentTimeline,
                purchaseCursor,
                actionUsage,
                emergencyOrderState,
                resolvedEvents,
                effectiveNow
        );

        gameDaySnapshotRedisRepository.save(
                user.getId(),
                store.getSeason().getId(),
                day,
                new GameDaySnapshot(
                        snapshot.storeId(),
                        snapshot.seasonId(),
                        snapshot.day(),
                        snapshot.locationId(),
                        snapshot.menuId(),
                        snapshot.price(),
                        snapshot.orderCount(),
                        snapshot.dailySeed(),
                        snapshot.purchaseList(),
                        purchaseCursor,
                        snapshot.response(),
                        snapshot.startedAt(),
                        effectiveNow
                )
        );

        return new GameStateResponse(
                serverTime,
                store.getSeason().getId(),
                day,
                String.valueOf(calculatedState.population()),
                responseLastCalculatedAt,
                calculatedState.cash(),
                purchaseCursor,
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
        );
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

    private GameDaySnapshot normalizeSnapshot(GameDaySnapshot snapshot, Order dailyStartOrder) {
        LocalDateTime startedAt = snapshot.startedAt();
        if (startedAt == null) {
            startedAt = dailyStartOrder != null && dailyStartOrder.getCreatedAt() != null
                    ? dailyStartOrder.getCreatedAt()
                    : LocalDateTime.now(clock);
        }

        LocalDateTime lastCalculatedAt = snapshot.lastCalculatedAt();
        if (lastCalculatedAt == null || lastCalculatedAt.isBefore(startedAt)) {
            lastCalculatedAt = startedAt;
        }

        int purchaseCursor = snapshot.purchaseCursor() == null ? 0 : snapshot.purchaseCursor();
        return new GameDaySnapshot(
                snapshot.storeId(),
                snapshot.seasonId(),
                snapshot.day(),
                snapshot.locationId(),
                snapshot.menuId(),
                snapshot.price(),
                snapshot.orderCount(),
                snapshot.dailySeed(),
                snapshot.purchaseList(),
                purchaseCursor,
                snapshot.response(),
                startedAt,
                lastCalculatedAt
        );
    }

    private int resolvePurchaseCursor(
            GameDaySnapshot snapshot,
            SeasonTimeline.DayTimeline currentTimeline,
            LocalDateTime effectiveNow
    ) {
        List<Integer> purchaseList = snapshot.purchaseList();
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
        return Math.min(purchaseList.size(), Math.max(snapshot.purchaseCursor(), computedCursor));
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
        for (Order emergencyOrder : emergencyOrders) {
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

        return new EmergencyOrderState(pendingArriveAt != null, pendingArriveAt, arrivedStock);
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
            GameDaySnapshot snapshot,
            SeasonTimeline.DayTimeline currentTimeline,
            int purchaseCursor,
            ActionUsage actionUsage,
            EmergencyOrderState emergencyOrderState,
            List<ResolvedEvent> resolvedEvents,
            LocalDateTime effectiveNow
    ) {
        long demandUnits = calculateDemandUnits(snapshot.purchaseList(), purchaseCursor);
        long capitalChange = 0L;
        int stockChange = emergencyOrderState.arrivedStock();
        BigDecimal populationEventMultiplier = DECIMAL_ONE;

        List<GameStateResponse.AppliedEvent> responseAppliedEvents = new ArrayList<>();
        for (ResolvedEvent resolvedEvent : resolvedEvents) {
            RandomEvent event = resolvedEvent.dailyEvent().getEvent();

            if (resolvedEvent.appliedDay() == snapshot.day()) {
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

        int totalAvailableStock = Math.max(0, snapshot.response().initialStock() + stockChange);
        long actualSoldUnits = Math.min(demandUnits, totalAvailableStock);
        int remainingStock = (int) Math.max(0L, totalAvailableStock - actualSoldUnits);
        long cash = snapshot.response().initialBalance()
                + Math.multiplyExact(actualSoldUnits, snapshot.price().longValue())
                + capitalChange
                - actionUsage.totalCost();

        int population = calculateCurrentPopulation(snapshot.response(), currentTimeline, populationEventMultiplier, effectiveNow);
        return new CalculatedGameState(cash, remainingStock, population, responseAppliedEvents);
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

        List<RealtimeSegment> realtimeSegments = buildRealtimeSegments(startResponse.hourlySchedule());
        if (realtimeSegments.isEmpty()) {
            return 0;
        }

        long totalMillis = SEASON_TIMELINE_POLICY.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int segmentIndex = (int) Math.min(
                realtimeSegments.size() - 1L,
                (boundedElapsedMillis * realtimeSegments.size()) / totalMillis
        );
        RealtimeSegment segment = realtimeSegments.get(segmentIndex);

        BigDecimal population = BigDecimal.valueOf(segment.population())
                .multiply(normalizeRate(startResponse.weatherMultiplier()))
                .multiply(segment.trafficMultiplier())
                .multiply(populationEventMultiplier);
        return population.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private List<RealtimeSegment> buildRealtimeSegments(Map<String, GameDayStartResponse.HourlySchedule> hourlySchedule) {
        if (hourlySchedule == null || hourlySchedule.isEmpty()) {
            return List.of();
        }

        List<GameDayStartResponse.HourlySchedule> schedules = new ArrayList<>(hourlySchedule.values());
        int segmentSize = Math.max(1, schedules.size() / SeasonTimeline.REALTIME_SEGMENT_COUNT);
        List<RealtimeSegment> segments = new ArrayList<>();

        for (int segmentIndex = 0; segmentIndex < SeasonTimeline.REALTIME_SEGMENT_COUNT; segmentIndex++) {
            int fromIndex = segmentIndex * segmentSize;
            if (fromIndex >= schedules.size()) {
                break;
            }
            int toIndex = segmentIndex == SeasonTimeline.REALTIME_SEGMENT_COUNT - 1
                    ? schedules.size()
                    : Math.min(schedules.size(), fromIndex + segmentSize);

            int populationSum = 0;
            List<BigDecimal> trafficMultipliers = new ArrayList<>();
            for (int index = fromIndex; index < toIndex; index++) {
                GameDayStartResponse.HourlySchedule schedule = schedules.get(index);
                populationSum += schedule.population();
                trafficMultipliers.add(normalizeRate(schedule.trafficMultiplier()));
            }
            segments.add(new RealtimeSegment(populationSum, average(trafficMultipliers)));
        }
        return segments;
    }

    private int toWholeNumber(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return DECIMAL_ONE;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return DECIMAL_ONE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
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
            int arrivedStock
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

    private record RealtimeSegment(
            int population,
            BigDecimal trafficMultiplier
    ) {
    }

    private record CalculatedGameState(
            long cash,
            int totalStock,
            int population,
            List<GameStateResponse.AppliedEvent> appliedEvents
    ) {
    }
}
