package com.ssafy.S14P21A205.news.service;

import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.news.dto.MenuMentionCount;
import com.ssafy.S14P21A205.news.entity.NewsArticle;
import com.ssafy.S14P21A205.news.entity.NewsCategory;
import com.ssafy.S14P21A205.news.entity.NewsReport;
import com.ssafy.S14P21A205.news.repository.NewsArticleRepository;
import com.ssafy.S14P21A205.news.repository.NewsReportRepository;
import com.ssafy.S14P21A205.news.service.AiNewsGenerator.NewsGenerationResult;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * NewsService에서 트랜잭션이 필요한 DB 저장 로직을 분리.
 * Spring AOP 프록시는 self-invocation을 인터셉트하지 못하므로,
 * @Transactional이 제대로 동작하려면 별도 빈에서 호출해야 함.
 */
@Service
@RequiredArgsConstructor
public class NewsDataSaver {

    private static final Logger log = LoggerFactory.getLogger(NewsDataSaver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiNewsGenerator aiNewsGenerator;
    private final NewsReportRepository newsReportRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final PopulationRepository populationRepository;
    private final DailyReportRepository dailyReportRepository;
    private final StoreRepository storeRepository;
    private final DailyEventRepository dailyEventRepository;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void saveNewsData(Long seasonId, Season season, int totalDays,
                             Map<Integer, List<MenuMentionCount>> dayMentions) {
        if (newsReportRepository.existsBySeasonId(seasonId)) {
            log.info("News already generated for season {} (race check)", seasonId);
            return;
        }

        String trafficRanking = buildAreaTrafficRanking();
        log.info("[NEWS] Step 3/4: Generating news for {} days via AI", totalDays);

        for (int day = 1; day <= totalDays; day++) {
            List<MenuMentionCount> mentions = dayMentions.getOrDefault(day, List.of());

            String trendRanking = convertMentionsToJson(mentions);
            NewsReport report = NewsReport.create(
                    season, day, "[]", trafficRanking, "[]", trendRanking, "[]");
            newsReportRepository.save(report);

            if (!mentions.isEmpty()) {
                log.info("[NEWS] Calling AI for trend news day {}/{}", day, totalDays);
                NewsGenerationResult result = aiNewsGenerator.generateTrendNews(seasonId, day, mentions);
                NewsArticle article = NewsArticle.create(
                        report, day, NewsCategory.TREND, result.title(), result.content());
                newsArticleRepository.save(article);
                log.info("[NEWS] Generated trend news for season {} day {}: {}", seasonId, day, result.title());
            }

            try {
                generateEventPreviewNews(report, seasonId, day, totalDays);
            } catch (Exception e) {
                log.error("Failed to generate event preview news. seasonId={} day={}", seasonId, day, e);
            }
        }
        log.info("[NEWS] Step 4/4: All news generated for season {}", seasonId);
    }

    @Transactional
    public void updateDayRankings(Long seasonId, int day) {
        NewsReport report = newsReportRepository.findBySeasonIdAndDay(seasonId, day).orElse(null);
        if (report == null) {
            return;
        }
        if (!"[]".equals(report.getAreaRevenueRanking())) {
            return;
        }

        String revenueRanking = buildAreaRevenueRanking(seasonId, day);
        String menuEntryRanking = buildMenuEntryRanking(seasonId);
        String areaEntryRanking = buildAreaEntryRanking(seasonId);

        report.updateRankings(revenueRanking, menuEntryRanking, areaEntryRanking);
        log.info("Updated rankings for season {} day {}", seasonId, day);

        Long reportId = report.getId();
        CompletableFuture.runAsync(() -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    NewsReport rpt = newsReportRepository.findById(reportId).orElse(null);
                    if (rpt != null) {
                        log.info("[NEWS-ASYNC] Starting closing news for season {} day {}", seasonId, day);
                        generateClosingNewsInternal(rpt, seasonId, day);
                        log.info("[NEWS-ASYNC] Completed closing news for season {} day {}", seasonId, day);
                    }
                });
            } catch (Exception e) {
                log.error("Async closing news generation failed. seasonId={} day={}", seasonId, day, e);
            }
        });
    }

    /**
     * 영업 중 뉴스 생성 (메뉴 입점수 + 지역 입점수). 비동기로 실행.
     */
    public void generateOpeningNews(Long seasonId, int day) {
        NewsReport report = newsReportRepository.findBySeasonIdAndDay(seasonId, day).orElse(null);
        if (report == null) {
            return;
        }

        Long reportId = report.getId();
        CompletableFuture.runAsync(() -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    NewsReport rpt = newsReportRepository.findById(reportId).orElse(null);
                    if (rpt != null) {
                        log.info("[NEWS-ASYNC] Starting opening news for season {} day {}", seasonId, day);
                        generateOpeningNewsInternal(rpt, seasonId, day);
                        log.info("[NEWS-ASYNC] Completed opening news for season {} day {}", seasonId, day);
                    }
                });
            } catch (Exception e) {
                log.error("Async opening news generation failed. seasonId={} day={}", seasonId, day, e);
            }
        });
    }

    // ---- 영업 중 뉴스 (메뉴 입점수 + 지역 입점수) ----

    private void generateOpeningNewsInternal(NewsReport report, Long seasonId, int day) {
        try {
            generateMenuEntryNews(report, seasonId, day);
        } catch (Exception e) {
            log.error("Failed to generate menu entry news. seasonId={} day={}", seasonId, day, e);
        }

        try {
            generateAreaEntryNews(report, seasonId, day);
        } catch (Exception e) {
            log.error("Failed to generate area entry news. seasonId={} day={}", seasonId, day, e);
        }
    }

    // ---- 마감 뉴스 (팝업 이동 / 매출 1위 / 누적 판매량 중 1건) ----

    private void generateClosingNewsInternal(NewsReport report, Long seasonId, int day) {
        List<Runnable> candidates = new ArrayList<>();
        candidates.add(() -> generateTopStoreNews(report, seasonId, day));
        candidates.add(() -> generateCumulativeSalesNews(report, seasonId, day));
        candidates.add(() -> generateMigrationNews(report, seasonId, day));

        Collections.shuffle(candidates);
        try {
            candidates.get(0).run();
        } catch (Exception e) {
            log.error("Failed to generate closing news. seasonId={} day={}", seasonId, day, e);
        }
    }

    private void generateMenuEntryNews(NewsReport report, Long seasonId, int day) {
        List<Object[]> rows = storeRepository.countStoresByMenu(seasonId);
        if (rows.isEmpty()) {
            return;
        }

        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("storeCount", ((Number) row[1]).longValue());
                    return item;
                })
                .toList();

        NewsGenerationResult result = aiNewsGenerator.generateMenuEntryNews(seasonId, day, ranking);
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.MENU_ENTRY, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated menu entry news for season {} day {}", seasonId, day);
    }

    private void generateAreaEntryNews(NewsReport report, Long seasonId, int day) {
        List<Object[]> rows = storeRepository.countStoresByLocation(seasonId);
        if (rows.isEmpty()) {
            return;
        }

        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("storeCount", ((Number) row[1]).longValue());
                    return item;
                })
                .toList();

        NewsGenerationResult result = aiNewsGenerator.generateAreaEntryNews(seasonId, day, ranking);
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.AREA_ENTRY, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated area entry news for season {} day {}", seasonId, day);
    }

    private void generateEventPreviewNews(NewsReport report, Long seasonId, int day, int totalDays) {
        if (day + 1 > totalDays) {
            return;
        }

        List<DailyEvent> upcomingEvents = dailyEventRepository
                .findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(seasonId, day + 1, day + 1);
        if (upcomingEvents.isEmpty()) {
            return;
        }

        // FESTIVAL 이벤트만 필터링
        List<DailyEvent> festivalEvents = upcomingEvents.stream()
                .filter(e -> "FESTIVAL".equals(e.getEvent().getEventType()))
                .toList();
        if (festivalEvents.isEmpty()) {
            return;
        }

        List<Map<String, Object>> eventData = festivalEvents.stream()
                .map(event -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("day", event.getDay());
                    item.put("daysUntil", event.getDay() - day);
                    item.put("eventType", event.getEvent().getEventType());
                    item.put("category", event.getEvent().getEventCategory().name());
                    return item;
                })
                .toList();

        NewsGenerationResult result = aiNewsGenerator.generateEventPreviewNews(seasonId, day, eventData);
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.EXTRA, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated event preview news for season {} day {}", seasonId, day);
    }

    private void generateTopStoreNews(NewsReport report, Long seasonId, int day) {
        if (day < 1) {
            return;
        }

        List<DailyReport> topStores = dailyReportRepository
                .findTopBySeasonIdAndDayOrderByRevenueDesc(seasonId, day);
        if (topStores.isEmpty()) {
            return;
        }

        DailyReport topStore = topStores.get(0);
        String storeName = topStore.getStore().getStoreName();
        String menuName = topStore.getMenuName();
        int revenue = topStore.getRevenue();
        int salesCount = topStore.getSalesCount();

        NewsGenerationResult result = aiNewsGenerator.generateTopStoreNews(
                seasonId, day, storeName, menuName, revenue, salesCount);
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.EXTRA, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated top store news for season {} day {}", seasonId, day);
    }

    private void generateCumulativeSalesNews(NewsReport report, Long seasonId, int day) {
        List<Object[]> salesData = dailyReportRepository.sumSalesCountBySeasonId(seasonId);
        if (salesData.isEmpty()) {
            return;
        }

        List<Map<String, Object>> milestones = new ArrayList<>();
        int[] thresholds = {100, 200, 500, 1000};

        for (Object[] row : salesData) {
            String storeName = (String) row[1];
            String menuName = (String) row[2];
            long totalSales = ((Number) row[3]).longValue();

            for (int threshold : thresholds) {
                if (totalSales >= threshold) {
                    Map<String, Object> milestone = new LinkedHashMap<>();
                    milestone.put("storeName", storeName);
                    milestone.put("menuName", menuName);
                    milestone.put("totalSales", totalSales);
                    milestone.put("milestone", threshold);
                    milestones.add(milestone);
                    break;
                }
            }
        }

        if (milestones.isEmpty()) {
            return;
        }

        Map<String, Object> topMilestone = milestones.get(0);
        NewsGenerationResult result = aiNewsGenerator.generateCumulativeSalesNews(
                seasonId, day,
                (String) topMilestone.get("storeName"),
                (String) topMilestone.get("menuName"),
                ((Number) topMilestone.get("totalSales")).longValue(),
                ((Number) topMilestone.get("milestone")).intValue());
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.EXTRA, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated cumulative sales news for season {} day {}", seasonId, day);
    }

    private void generateMigrationNews(NewsReport report, Long seasonId, int day) {
        if (day < 2) {
            return;
        }

        List<Object[]> currentLocationCounts = dailyReportRepository
                .countStoresByLocationAndDay(seasonId, day);
        List<Object[]> previousLocationCounts = dailyReportRepository
                .countStoresByLocationAndDay(seasonId, day - 1);

        if (currentLocationCounts.isEmpty() || previousLocationCounts.isEmpty()) {
            return;
        }

        Map<String, Long> prevMap = new LinkedHashMap<>();
        for (Object[] row : previousLocationCounts) {
            prevMap.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> changes = new ArrayList<>();
        for (Object[] row : currentLocationCounts) {
            String locationName = (String) row[0];
            long currentCount = ((Number) row[1]).longValue();
            long previousCount = prevMap.getOrDefault(locationName, 0L);
            long diff = currentCount - previousCount;
            if (diff != 0) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("name", locationName);
                change.put("currentCount", currentCount);
                change.put("previousCount", previousCount);
                change.put("change", diff);
                changes.add(change);
            }
        }

        if (changes.isEmpty()) {
            return;
        }

        NewsGenerationResult result = aiNewsGenerator.generateMigrationNews(seasonId, day, changes);
        NewsArticle article = NewsArticle.create(
                report, day, NewsCategory.EXTRA, result.title(), result.content());
        newsArticleRepository.save(article);
        log.info("Generated migration news for season {} day {}", seasonId, day);
    }

    // ---- Ranking build methods ----

    private String buildAreaTrafficRanking() {
        List<Object[]> rows = populationRepository.avgPopulationByLocation();
        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("avgPopulation", ((Number) row[1]).doubleValue());
                    return item;
                })
                .toList();
        return toJson(ranking);
    }

    private String buildAreaRevenueRanking(Long seasonId, int day) {
        List<Object[]> rows = dailyReportRepository.sumRevenueByLocationAndDay(seasonId, day);
        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("revenue", ((Number) row[1]).longValue());
                    return item;
                })
                .toList();
        return toJson(ranking);
    }

    private String buildMenuEntryRanking(Long seasonId) {
        List<Object[]> rows = storeRepository.countStoresByMenu(seasonId);
        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("storeCount", ((Number) row[1]).longValue());
                    return item;
                })
                .toList();
        return toJson(ranking);
    }

    private String buildAreaEntryRanking(Long seasonId) {
        List<Object[]> rows = storeRepository.countStoresByLocation(seasonId);
        List<Map<String, Object>> ranking = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", row[0]);
                    item.put("storeCount", ((Number) row[1]).longValue());
                    return item;
                })
                .toList();
        return toJson(ranking);
    }

    // ---- Utility methods ----

    private String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String convertMentionsToJson(List<MenuMentionCount> mentions) {
        try {
            return MAPPER.writeValueAsString(mentions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
