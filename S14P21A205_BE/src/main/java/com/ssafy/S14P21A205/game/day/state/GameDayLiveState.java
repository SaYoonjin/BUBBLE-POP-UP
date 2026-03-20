package com.ssafy.S14P21A205.game.day.state;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GameDayLiveState(
        LocalDateTime startedAt,
        List<Integer> purchaseList,
        Integer purchaseCursor,
        GameDayStartResponse startResponse,
        Integer tick,
        Integer regionStoreCount,
        Integer populationPerStore,
        BigDecimal captureRate,
        Integer salePrice,
        Integer tickCustomerCount,
        List<Integer> tickSoldUnits,
        Integer tickPurchaseCount,
        Long tickSales,
        Integer cumulativeCustomerCount,
        Integer cumulativePurchaseCount,
        Long cumulativeSales,
        Long cumulativeTotalCost,
        Long locationChangeCost,
        Long balance,
        Integer stock,
        LocalDateTime lastCalculatedAt
) {

    public GameDayLiveState(
            LocalDateTime startedAt,
            List<Integer> purchaseList,
            Integer purchaseCursor,
            GameDayStartResponse startResponse,
            Integer tick,
            Integer populationPerStore,
            BigDecimal captureRate,
            Integer salePrice,
            Integer tickCustomerCount,
            List<Integer> tickSoldUnits,
            Integer tickPurchaseCount,
            Long tickSales,
            Integer cumulativeCustomerCount,
            Integer cumulativePurchaseCount,
            Long cumulativeSales,
            Long cumulativeTotalCost,
            Long balance,
            Integer stock,
            LocalDateTime lastCalculatedAt
    ) {
        this(
                startedAt,
                purchaseList,
                purchaseCursor,
                startResponse,
                tick,
                null,
                populationPerStore,
                captureRate,
                salePrice,
                tickCustomerCount,
                tickSoldUnits,
                tickPurchaseCount,
                tickSales,
                cumulativeCustomerCount,
                cumulativePurchaseCount,
                cumulativeSales,
                cumulativeTotalCost,
                0L,
                balance,
                stock,
                lastCalculatedAt
        );
    }

    public GameDayLiveState(
            Long cumulativeSales,
            Long cumulativeTotalCost,
            Integer stock,
            LocalDateTime lastCalculatedAt
    ) {
        this(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                cumulativeSales,
                cumulativeTotalCost,
                0L,
                null,
                stock,
                lastCalculatedAt
        );
    }
}
