package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.news.entity.NewsReport;
import com.ssafy.S14P21A205.game.news.repository.NewsReportRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NewsRankingResolver {

    private final NewsReportRepository newsReportRepository;
    private final ObjectMapper objectMapper;

    public PreviousDayRanking resolve(Store store, int day) {
        return findPreviousReport(store.getSeason().getId(), day)
                .map(report -> new PreviousDayRanking(
                        resolveAreaEntryRank(report, store.getLocation()),
                        resolveMenuEntryRank(report, store.getMenu()),
                        resolveTrendKeywordRank(report, store.getMenu())
                ))
                .orElse(new PreviousDayRanking(null, null, null));
    }

    public Integer resolveAreaEntryRank(Long seasonId, int day, Location location) {
        return findPreviousReport(seasonId, day)
                .map(report -> resolveAreaEntryRank(report, location))
                .orElse(null);
    }

    public Integer resolveMenuEntryRank(Long seasonId, int day, Menu menu) {
        return findPreviousReport(seasonId, day)
                .map(report -> resolveMenuEntryRank(report, menu))
                .orElse(null);
    }

    public Integer resolveTrendKeywordRank(Long seasonId, int day, Menu menu) {
        return findPreviousReport(seasonId, day)
                .map(report -> resolveTrendKeywordRank(report, menu))
                .orElse(null);
    }

    private Integer resolveAreaEntryRank(NewsReport report, Location location) {
        return resolveRank(report.getAreaEntryRanking(), location.getId(), location.getLocationName(), true);
    }

    private Integer resolveMenuEntryRank(NewsReport report, Menu menu) {
        return resolveRank(report.getMenuEntryRanking(), menu.getId(), menu.getMenuName(), false);
    }

    private Integer resolveTrendKeywordRank(NewsReport report, Menu menu) {
        return resolveRank(report.getTrendKeywordRanking(), menu.getId(), menu.getMenuName(), false);
    }

    private Optional<NewsReport> findPreviousReport(Long seasonId, int day) {
        if (day <= 1) {
            return Optional.empty();
        }
        return newsReportRepository.findFirstBySeason_IdAndDay(seasonId, day - 1);
    }

    private Integer resolveRank(String payload, Long targetId, String targetName, boolean areaRanking) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            return resolveRank(root, targetId, targetName, areaRanking);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resolveRank(JsonNode node, Long targetId, String targetName, boolean areaRanking) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                JsonNode element = node.get(index);
                Integer explicitRank = readInteger(element, "rank").orElse(index + 1);
                if (matches(element, targetId, targetName, areaRanking)) {
                    return explicitRank;
                }
            }
            return null;
        }

        if (node.isObject()) {
            Integer directRank = readInteger(node, "rank").orElse(null);
            if (directRank != null && matches(node, targetId, targetName, areaRanking)) {
                return directRank;
            }

            for (Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator(); iterator.hasNext();) {
                Map.Entry<String, JsonNode> field = iterator.next();
                JsonNode value = field.getValue();

                if (value.isArray() || value.isObject()) {
                    Integer nestedRank = resolveRank(value, targetId, targetName, areaRanking);
                    if (nestedRank != null) {
                        return nestedRank;
                    }
                    continue;
                }

                Integer keyedRank = parseInteger(field.getKey()).orElse(null);
                if (keyedRank != null && matches(value, targetId, targetName, areaRanking)) {
                    return keyedRank;
                }
            }
        }

        return matches(node, targetId, targetName, areaRanking) ? 1 : null;
    }

    private boolean matches(JsonNode node, Long targetId, String targetName, boolean areaRanking) {
        if (node == null || node.isNull()) {
            return false;
        }

        if (node.isNumber()) {
            return targetId != null && node.longValue() == targetId;
        }

        if (node.isTextual()) {
            String value = node.asText();
            return matchesIdText(value, targetId) || matchesNameText(value, targetName);
        }

        if (!node.isObject()) {
            return false;
        }

        String[] idKeys = areaRanking
                ? new String[]{"locationId", "areaId", "id"}
                : new String[]{"menuId", "keywordId", "id"};
        String[] nameKeys = areaRanking
                ? new String[]{"locationName", "areaName", "name"}
                : new String[]{"menuName", "keyword", "keywordName", "name"};

        for (String idKey : idKeys) {
            if (readLong(node, idKey).filter(value -> value.equals(targetId)).isPresent()) {
                return true;
            }
            if (readText(node, idKey).filter(value -> matchesIdText(value, targetId)).isPresent()) {
                return true;
            }
        }

        for (String nameKey : nameKeys) {
            if (readText(node, nameKey).filter(value -> matchesNameText(value, targetName)).isPresent()) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesIdText(String value, Long targetId) {
        return targetId != null && StringUtils.hasText(value) && value.trim().equals(String.valueOf(targetId));
    }

    private boolean matchesNameText(String value, String targetName) {
        return StringUtils.hasText(value)
                && StringUtils.hasText(targetName)
                && value.trim().equalsIgnoreCase(targetName.trim());
    }

    private Optional<Long> readLong(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (value.isNumber()) {
            return Optional.of(value.longValue());
        }
        return parseLong(value.asText());
    }

    private Optional<Integer> readInteger(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (value.isNumber()) {
            return Optional.of(value.intValue());
        }
        return parseInteger(value.asText());
    }

    private Optional<String> readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? Optional.of(text) : Optional.empty();
    }

    private Optional<Integer> parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record PreviousDayRanking(
            Integer areaEntryRank,
            Integer menuEntryRank,
            Integer trendKeywordRank
    ) {
    }
}
