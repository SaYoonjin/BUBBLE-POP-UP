package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class StoreRankingPolicy {

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final BigDecimal RENT_MULTIPLIER_FIRST = new BigDecimal("1.30");
    private static final BigDecimal RENT_MULTIPLIER_SECOND = new BigDecimal("1.20");
    private static final BigDecimal RENT_MULTIPLIER_THIRD = new BigDecimal("1.10");
    private static final BigDecimal MENU_ENTRY_MULTIPLIER_FIRST = new BigDecimal("1.20");
    private static final BigDecimal MENU_ENTRY_MULTIPLIER_SECOND = new BigDecimal("1.10");
    private static final BigDecimal MENU_ENTRY_MULTIPLIER_NINTH = new BigDecimal("0.90");
    private static final BigDecimal MENU_ENTRY_MULTIPLIER_TENTH = new BigDecimal("0.80");
    private static final BigDecimal TREND_KEYWORD_MULTIPLIER_FIRST = new BigDecimal("1.20");
    private static final BigDecimal TREND_KEYWORD_MULTIPLIER_SECOND = new BigDecimal("1.10");
    private static final BigDecimal TREND_KEYWORD_MULTIPLIER_SEVENTH = new BigDecimal("0.90");
    private static final BigDecimal TREND_KEYWORD_MULTIPLIER_EIGHTH = new BigDecimal("0.80");

    public GameDayStartResponse.MarketSnapshot resolveSnapshot(
            Store store,
            List<Store> seasonStores,
            DaySchedule daySchedule,
            String festivalName,
            BigDecimal festivalMultiplier
    ) {
        return resolveSnapshot(store, seasonStores, daySchedule, festivalName, festivalMultiplier, null, null);
    }

    public GameDayStartResponse.MarketSnapshot resolveSnapshot(
            Store store,
            List<Store> seasonStores,
            DaySchedule daySchedule,
            String festivalName,
            BigDecimal festivalMultiplier,
            Integer locationPopularityRankOverride,
            Integer menuTrendRankOverride
    ) {
        int averageMenuPrice = resolveAverageMenuPrice(store.getMenu().getId(), store.getPrice(), seasonStores);
        int regionStoreCount = countByLocation(store.getLocation().getId(), seasonStores);
        int totalFloatingPopulation = daySchedule.hourlySchedule().values().stream()
                .map(schedule -> schedule.effectivePopulation() == null ? schedule.population() : schedule.effectivePopulation())
                .mapToInt(Integer::intValue)
                .sum();
        int totalPopulationPerStore = regionStoreCount <= 0
                ? totalFloatingPopulation
                : BigDecimal.valueOf(totalFloatingPopulation)
                .divide(BigDecimal.valueOf(regionStoreCount), 0, RoundingMode.HALF_UP)
                .intValue();
        int locationPopularityRank = locationPopularityRankOverride == null
                ? resolveLocationPopularityRank(store.getLocation().getId(), seasonStores)
                : locationPopularityRankOverride;
        int menuTrendRank = menuTrendRankOverride == null
                ? resolveMenuTrendRank(store.getMenu().getId(), seasonStores)
                : menuTrendRankOverride;
        PriceBand priceBand = resolvePriceBand(store.getPrice(), averageMenuPrice);
        BigDecimal trendMultiplier = resolveTrendMultiplier(menuTrendRank);

        return new GameDayStartResponse.MarketSnapshot(
                averageMenuPrice,
                regionStoreCount,
                totalFloatingPopulation,
                totalPopulationPerStore,
                locationPopularityRank,
                menuTrendRank,
                priceBand.ratio(),
                priceBand.label(),
                priceBand.multiplier(),
                trendMultiplier,
                festivalName,
                festivalMultiplier == null ? null : normalizeScale(festivalMultiplier)
        );
    }

    public int resolveLocationPopularityRank(Long locationId, List<Store> seasonStores) {
        return resolveRank(seasonStores, store -> store.getLocation().getId(), locationId);
    }

    public int resolveMenuTrendRank(Long menuId, List<Store> seasonStores) {
        return resolveRank(seasonStores, store -> store.getMenu().getId(), menuId);
    }

    // 같은 시즌 + 같은 메뉴 기준 평균가 구하기
    public int resolveAverageMenuPrice(Long menuId, int fallbackPrice, List<Store> seasonStores) {
        int priceSum = 0;
        int count = 0;
        for (Store seasonStore : seasonStores) {
            if (!seasonStore.getMenu().getId().equals(menuId) || seasonStore.getPrice() == null) {
                continue;
            }
            priceSum += seasonStore.getPrice();
            count++;
        }
        return count == 0 ? fallbackPrice : priceSum / count;
    }

    public BigDecimal resolveRentMultiplier(Integer rank) {
        if (rank == null || rank <= 0) {
            return DECIMAL_ONE;
        }
        if (rank == 1) {
            return RENT_MULTIPLIER_FIRST;
        }
        if (rank == 2) {
            return RENT_MULTIPLIER_SECOND;
        }
        if (rank == 3) {
            return RENT_MULTIPLIER_THIRD;
        }
        return DECIMAL_ONE;
    }

    public BigDecimal resolveTrendMultiplier(Integer rank) {
        return resolveMenuEntryMultiplier(rank);
    }

    public BigDecimal resolveMenuEntryMultiplier(Integer rank) {
        if (rank == null || rank <= 0) {
            return DECIMAL_ONE;
        }
        if (rank == 1) {
            return MENU_ENTRY_MULTIPLIER_FIRST;
        }
        if (rank == 2) {
            return MENU_ENTRY_MULTIPLIER_SECOND;
        }
        if (rank == 9) {
            return MENU_ENTRY_MULTIPLIER_NINTH;
        }
        if (rank == 10) {
            return MENU_ENTRY_MULTIPLIER_TENTH;
        }
        return DECIMAL_ONE;
    }

    public BigDecimal resolveTrendKeywordCaptureMultiplier(Integer rank) {
        if (rank == null || rank <= 0) {
            return DECIMAL_ONE;
        }
        if (rank == 1) {
            return TREND_KEYWORD_MULTIPLIER_FIRST;
        }
        if (rank == 2) {
            return TREND_KEYWORD_MULTIPLIER_SECOND;
        }
        if (rank == 7) {
            return TREND_KEYWORD_MULTIPLIER_SEVENTH;
        }
        if (rank == 8) {
            return TREND_KEYWORD_MULTIPLIER_EIGHTH;
        }
        return DECIMAL_ONE;
    }

    public int apply(int amount, BigDecimal... multipliers) {
        BigDecimal result = BigDecimal.valueOf(amount);
        for (BigDecimal multiplier : multipliers) {
            result = result.multiply(normalizePositive(multiplier));
        }
        return result.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public int applyPercentage(int amount, BigDecimal rate) {
        return BigDecimal.valueOf(amount)
                .multiply(normalizePositive(rate))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int countByLocation(Long locationId, List<Store> seasonStores) {
        int count = 0;
        for (Store seasonStore : seasonStores) {
            if (seasonStore.getLocation().getId().equals(locationId)) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private int resolveRank(List<Store> seasonStores, Function<Store, Long> keyExtractor, Long targetKey) {
        Map<Long, Long> counts = seasonStores.stream()
                .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));

        List<Map.Entry<Long, Long>> sorted = counts.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<Long, Long>, Long>comparing(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();

        for (int index = 0; index < sorted.size(); index++) {
            if (sorted.get(index).getKey().equals(targetKey)) {
                return index + 1;
            }
        }
        return 1;
    }

    private PriceBand resolvePriceBand(int sellingPrice, int averagePrice) {
        int denominator = averagePrice <= 0 ? Math.max(1, sellingPrice) : averagePrice;
        BigDecimal ratio = BigDecimal.valueOf(sellingPrice)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        BigDecimal averageUpperBound = new BigDecimal("1.10");
        BigDecimal averageLowerBound = new BigDecimal("0.90");

        if (ratio.compareTo(new BigDecimal("2.00")) > 0) {
            return new PriceBand(ratio, "OVER_200", new BigDecimal("0.01"));
        }
        if (ratio.compareTo(new BigDecimal("1.90")) > 0) {
            return new PriceBand(ratio, "OVER_190", new BigDecimal("0.10"));
        }
        if (ratio.compareTo(new BigDecimal("1.80")) > 0) {
            return new PriceBand(ratio, "OVER_180", new BigDecimal("0.20"));
        }
        if (ratio.compareTo(new BigDecimal("1.70")) > 0) {
            return new PriceBand(ratio, "OVER_170", new BigDecimal("0.30"));
        }
        if (ratio.compareTo(new BigDecimal("1.60")) > 0) {
            return new PriceBand(ratio, "OVER_160", new BigDecimal("0.40"));
        }
        if (ratio.compareTo(new BigDecimal("1.50")) > 0) {
            return new PriceBand(ratio, "OVER_150", new BigDecimal("0.50"));
        }
        if (ratio.compareTo(new BigDecimal("1.40")) > 0) {
            return new PriceBand(ratio, "OVER_140", new BigDecimal("0.60"));
        }
        if (ratio.compareTo(new BigDecimal("1.30")) > 0) {
            return new PriceBand(ratio, "OVER_130", new BigDecimal("0.70"));
        }
        if (ratio.compareTo(new BigDecimal("1.20")) > 0) {
            return new PriceBand(ratio, "OVER_120", new BigDecimal("0.80"));
        }
        if (ratio.compareTo(averageUpperBound) > 0) {
            return new PriceBand(ratio, "OVER_110_TO_120", new BigDecimal("0.80"));
        }
        if (ratio.compareTo(averageLowerBound) >= 0) {
            return new PriceBand(ratio, "AVERAGE", DECIMAL_ONE);
        }
        if (ratio.compareTo(new BigDecimal("0.80")) < 0 && ratio.compareTo(new BigDecimal("0.70")) >= 0) {
            return new PriceBand(ratio, "UNDER_80", new BigDecimal("1.20"));
        }
        if (ratio.compareTo(new BigDecimal("0.70")) < 0 && ratio.compareTo(new BigDecimal("0.60")) >= 0) {
            return new PriceBand(ratio, "UNDER_70", new BigDecimal("1.30"));
        }
        if (ratio.compareTo(new BigDecimal("0.60")) < 0) {
            return new PriceBand(ratio, "UNDER_60", new BigDecimal("1.40"));
        }
        return new PriceBand(ratio, "UNDER_80_TO_90", new BigDecimal("1.20"));
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        return value == null ? DECIMAL_ONE : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return DECIMAL_ONE;
        }
        return value;
    }

    private record PriceBand(
            BigDecimal ratio,
            String label,
            BigDecimal multiplier
    ) {
    }
}
