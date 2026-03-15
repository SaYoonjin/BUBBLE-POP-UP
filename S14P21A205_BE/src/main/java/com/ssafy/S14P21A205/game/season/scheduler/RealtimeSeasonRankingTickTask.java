package com.ssafy.S14P21A205.game.season.scheduler;

import com.ssafy.S14P21A205.game.day.dto.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.scheduler.GameTickTask;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingItemResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRedisRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(300)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeSeasonRankingTickTask implements GameTickTask {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSeasonRankingTickTask.class);
    private static final BigDecimal ZERO_ROI = new BigDecimal("0.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter REFRESHED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SeasonRepository seasonRepository;
    private final StoreRepository storeRepository;
    private final DailyReportRepository dailyReportRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;
    private final SeasonRankingRedisRepository seasonRankingRedisRepository;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public String taskName() {
        return "realtimeSeasonRanking";
    }

    @Override
    public void execute() {
        refreshCurrentTopRankings();
    }

    public void refreshCurrentTopRankings() {
        try {
            // 현재 진행 중 시즌 조회
            Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS).orElse(null);
            if (season == null) {
                seasonRankingRedisRepository.deleteCurrentTopRankings();
                log.info("Realtime season rankings cache cleared. reason=no_in_progress_season");
                return;
            }

            // 현재 day 계산
            int currentDay = resolveCurrentDay(season);
            List<Store> stores = storeRepository.findBySeason_IdOrderByIdAsc(season.getId());
            if (stores.isEmpty()) {
                seasonRankingRedisRepository.deleteCurrentTopRankings();
                log.info(
                        "Realtime season rankings cache cleared. seasonId={} currentDay={} reason=no_stores",
                        season.getId(),
                        currentDay
                );
                return;
            }

            // 과거 day(현재 day 이전)의 누적 매출/비용을 DailyReport에서 집계
            Map<Long, AggregatedStats> pastStatsByStoreId = aggregatePastStats(
                    dailyReportRepository.findByStore_Season_IdAndDayLessThanOrderByStore_IdAscDayAsc(season.getId(), currentDay)
            );
            // 오늘 day의 실시간 매출/비용을 Redis에서 조회
            Map<Long, LiveDayStats> todayStatsByStoreId = loadTodayStatsByStoreId(stores, currentDay);

            // 가게별 총 매출/총 비용/ROI 계산
            List<LiveRankingCandidate> candidates = new ArrayList<>();
            for (Store store : stores) {
                AggregatedStats pastStats = pastStatsByStoreId.getOrDefault(store.getId(), AggregatedStats.empty());
                LiveDayStats todayStats = todayStatsByStoreId.getOrDefault(store.getId(), LiveDayStats.empty());

                long totalRevenue = pastStats.totalRevenue() + todayStats.totalRevenue();
                long totalCost = pastStats.totalCost() + todayStats.totalCost();
                candidates.add(new LiveRankingCandidate(store, totalRevenue, totalCost, calculateRoi(totalRevenue, totalCost)));
            }
            // 정렬
            // 1) ROI 내림차순   2) userId 오름차순   3)storeId 오름차순
            candidates.sort(Comparator
                    .comparing(LiveRankingCandidate::roi, Comparator.reverseOrder())
                    .thenComparing(candidate -> candidate.store().getUser().getId())
                    .thenComparing(candidate -> candidate.store().getId()));

            List<CurrentSeasonTopRankingItemResponse> rankings = new ArrayList<>();
            int limit = Math.min(10, candidates.size());
            BigDecimal previousRoi = null;
            int currentRank = 0;
            for (int index = 0; index < limit; index++) {
                LiveRankingCandidate candidate = candidates.get(index);

                // 같은 ROI면 공동 순위 처리
                if (previousRoi == null || candidate.roi().compareTo(previousRoi) != 0) {
                    currentRank = index + 1;
                    previousRoi = candidate.roi();
                }
                rankings.add(new CurrentSeasonTopRankingItemResponse(
                        currentRank,
                        candidate.store().getUser().getId(),
                        candidate.store().getUser().getNickname(),
                        candidate.store().getStoreName(),
                        candidate.roi(),
                        candidate.totalRevenue(),
                        resolveRewardPoints(currentRank)
                ));
            }

            if (rankings.isEmpty()) {
                seasonRankingRedisRepository.deleteCurrentTopRankings();
                log.info(
                        "Realtime season rankings cache cleared. seasonId={} currentDay={} reason=no_rankings_generated",
                        season.getId(),
                        currentDay
                );
                return;
            }

            String refreshedAt = resolveRefreshedAt(todayStatsByStoreId);
            seasonRankingRedisRepository.saveCurrentTopRankings(new CurrentSeasonTopRankingsResponse(
                    season.getId(),
                    rankings,
                    refreshedAt
            ));
            log.info(
                    "Realtime season rankings refreshed. seasonId={} currentDay={} storeCount={} rankingCount={} refreshedAt={} topRankings={}",
                    season.getId(),
                    currentDay,
                    stores.size(),
                    rankings.size(),
                    refreshedAt,
                    summarizeTopRankings(rankings, candidates, 3)
            );
        } catch (Exception e) {
            log.error("Failed to refresh realtime season rankings.", e);
        }
    }

    private int resolveCurrentDay(Season season) {
        int currentDay = season.getCurrentDay() == null ? 1 : season.getCurrentDay();
        if (currentDay < 1 || currentDay > season.getTotalDays()) {
            throw new IllegalStateException("Current season day is out of range.");
        }
        return currentDay;
    }

    // 현재 day 이전까지의 DB 일일 리포트를 가게별로 누적합
    private Map<Long, AggregatedStats> aggregatePastStats(List<DailyReport> dailyReports) {
        Map<Long, AggregatedStats> statsByStoreId = new HashMap<>();
        for (DailyReport dailyReport : dailyReports) {
            Long storeId = dailyReport.getStore().getId();
            AggregatedStats current = statsByStoreId.getOrDefault(storeId, AggregatedStats.empty());
            statsByStoreId.put(
                    storeId,
                    new AggregatedStats(
                            current.totalRevenue() + valueOf(dailyReport.getRevenue()),
                            current.totalCost() + valueOf(dailyReport.getTotalCost())
                    )
            );
        }
        return statsByStoreId;
    }

    // 모든 가게에 대해 오늘 day의 실시간 상태를 Redis에서 조회
    private Map<Long, LiveDayStats> loadTodayStatsByStoreId(List<Store> stores, int currentDay) {
        Map<Long, LiveDayStats> todayStatsByStoreId = new HashMap<>();
        for (Store store : stores) {
            LiveDayStats liveDayStats = calculateLiveDayStats(store.getId(), currentDay);
            if (liveDayStats.isEmpty()) {
                continue;
            }
            todayStatsByStoreId.put(store.getId(), liveDayStats);
        }
        return todayStatsByStoreId;
    }
    // Load today's live revenue/cost state from Redis for a single store.
    private LiveDayStats calculateLiveDayStats(Long storeId, int currentDay) {
        return gameDayStoreStateRedisRepository.find(storeId, currentDay)
                .map(this::toLiveDayStats)
                .orElseGet(LiveDayStats::empty);
    }

    private LiveDayStats toLiveDayStats(GameDayLiveState state) {
        return new LiveDayStats(
                state.cumulativeSales(),
                state.cumulativeTotalCost(),
                state.lastCalculatedAt()
        );
    }

    private String resolveRefreshedAt(Map<Long, LiveDayStats> todayStatsByStoreId) {
        return todayStatsByStoreId.values().stream()
                .map(LiveDayStats::lastCalculatedAt)
                .filter(lastCalculatedAt -> lastCalculatedAt != null)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now(clock))
                .withNano(0)
                .format(REFRESHED_AT_FORMATTER);
    }

    private int resolveRewardPoints(int rank) {
        return switch (rank) {
            case 1 -> 30;
            case 2 -> 20;
            case 3 -> 10;
            default -> 5;
        };
    }

    private BigDecimal calculateRoi(long totalRevenue, long totalCost) {
        if (totalCost <= 0L) {
            return ZERO_ROI;
        }

        return BigDecimal.valueOf(totalRevenue - totalCost)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalCost), 1, RoundingMode.HALF_UP);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private String summarizeTopRankings(
            List<CurrentSeasonTopRankingItemResponse> rankings,
            List<LiveRankingCandidate> candidates,
            int limit
    ) {
        if (rankings == null || rankings.isEmpty() || candidates == null || candidates.isEmpty()) {
            return "[]";
        }

        int summarySize = Math.min(Math.min(limit, rankings.size()), candidates.size());
        StringBuilder summary = new StringBuilder("[");
        for (int index = 0; index < summarySize; index++) {
            CurrentSeasonTopRankingItemResponse ranking = rankings.get(index);
            LiveRankingCandidate candidate = candidates.get(index);
            if (index > 0) {
                summary.append(", ");
            }
            summary.append("#")
                    .append(ranking.rank())
                    .append("(userId=")
                    .append(ranking.userId())
                    .append(", storeId=")
                    .append(candidate.store().getId())
                    .append(", revenue=")
                    .append(ranking.totalRevenue())
                    .append(", cost=")
                    .append(candidate.totalCost())
                    .append(", roi=")
                    .append(ranking.roi())
                    .append(")");
        }
        summary.append("]");
        return summary.toString();
    }

    private record AggregatedStats(long totalRevenue, long totalCost) {
        private static AggregatedStats empty() {
            return new AggregatedStats(0L, 0L);
        }
    }

    private record LiveRankingCandidate(Store store, long totalRevenue, long totalCost, BigDecimal roi) {
    }

    private record LiveDayStats(long totalRevenue, long totalCost, LocalDateTime lastCalculatedAt) {
        private static LiveDayStats empty() {
            return new LiveDayStats(0L, 0L, null);
        }

        private boolean isEmpty() {
            return totalRevenue == 0L && totalCost == 0L && lastCalculatedAt == null;
        }
    }
}
