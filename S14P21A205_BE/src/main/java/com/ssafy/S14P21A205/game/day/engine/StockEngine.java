package com.ssafy.S14P21A205.game.day.engine;

import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StockEngine {

    private static final Duration TICK_INTERVAL = Duration.ofSeconds(10);
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public int resolveCurrentTick(DayWindow currentTimeline, LocalDateTime effectiveNow) {
        if (!effectiveNow.isAfter(currentTimeline.businessStart())) {
            return 0;
        }
        if (!effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return totalTickCount();
        }

        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        return (int) Math.min(totalTickCount(), Math.max(0L, elapsedMillis / TICK_INTERVAL.toMillis()));
    }

    public LocalDateTime resolveTickBoundary(DayWindow currentTimeline, int tick) {
        if (tick <= 0) {
            return currentTimeline.businessStart();
        }

        LocalDateTime tickBoundary = currentTimeline.businessStart().plus(TICK_INTERVAL.multipliedBy(tick));
        return tickBoundary.isAfter(currentTimeline.businessEnd()) ? currentTimeline.businessEnd() : tickBoundary;
    }

    public int calculateTickCustomerCount(int populationPerStore, BigDecimal captureRate) {
        if (populationPerStore <= 0 || captureRate == null || captureRate.signum() <= 0) {
            return 0;
        }

        return BigDecimal.valueOf(populationPerStore)
                .multiply(captureRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    public int advancePurchaseCursor(GameDayLiveState state, int tickCustomerCount) {
        return advancePurchaseCursor(
                state.purchaseList(),
                state.purchaseCursor() == null ? 0 : state.purchaseCursor(),
                tickCustomerCount
        );
    }

    public int advancePurchaseCursor(List<Integer> purchaseList, int purchaseCursor, int tickCustomerCount) {
        if (purchaseList == null || purchaseList.isEmpty() || tickCustomerCount <= 0) {
            return purchaseCursor;
        }
        return Math.min(purchaseList.size(), purchaseCursor + tickCustomerCount);
    }

    public int resolvePurchaseCursorAtTick(
            GameDayLiveState state,
            DayWindow currentTimeline,
            int tick
    ) {
        if (tick <= 0) {
            return 0;
        }

        LocalDateTime tickBoundary = resolveTickBoundary(currentTimeline, tick);
        return resolvePurchaseCursor(state, currentTimeline, tickBoundary);
    }

    public long calculateDemandUnits(List<Integer> purchaseList, int purchaseCursor) {
        return calculateDemandUnits(purchaseList, 0, purchaseCursor);
    }

    public long calculateDemandUnits(List<Integer> purchaseList, int fromCursor, int toCursor) {
        if (purchaseList == null || purchaseList.isEmpty() || toCursor <= fromCursor) {
            return 0L;
        }

        long demandUnits = 0L;
        int start = Math.max(0, Math.min(fromCursor, purchaseList.size()));
        int end = Math.max(start, Math.min(toCursor, purchaseList.size()));
        for (int index = start; index < end; index++) {
            demandUnits += purchaseList.get(index);
        }
        return demandUnits;
    }

    public StockCalculation calculateStock(
            GameDayLiveState state,
            int eventStockChange,
            int emergencyArrivedStock,
            long demandUnits
    ) {
        int totalAvailableStock = Math.max(0, state.startResponse().initialStock() + eventStockChange + emergencyArrivedStock);
        long actualSoldUnits = Math.min(demandUnits, totalAvailableStock);
        int remainingStock = (int) Math.max(0L, totalAvailableStock - actualSoldUnits);
        return new StockCalculation(totalAvailableStock, actualSoldUnits, remainingStock);
    }

    public TickProgress calculateTickProgress(
            GameDayLiveState state,
            DayWindow currentTimeline,
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

    private int resolvePurchaseCursor(
            GameDayLiveState state,
            DayWindow currentTimeline,
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

        long totalMillis = seasonTimelineService.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int computedCursor = (int) ((purchaseList.size() * boundedElapsedMillis) / totalMillis);
        int persistedCursor = state.purchaseCursor() == null ? 0 : state.purchaseCursor();
        return Math.min(purchaseList.size(), Math.max(persistedCursor, computedCursor));
    }

    private int totalTickCount() {
        return (int) (seasonTimelineService.businessDuration().toMillis() / TICK_INTERVAL.toMillis());
    }

    private int safeToInt(long value) {
        return Math.toIntExact(Math.max(0L, value));
    }

    public record TickProgress(
            int tickCustomerCount,
            int tickPurchaseCount,
            int cumulativeCustomerCount,
            long tickSales
    ) {
    }

    public record StockCalculation(
            int totalAvailableStock,
            long actualSoldUnits,
            int remainingStock
    ) {
    }
}
