package com.ssafy.S14P21A205.game.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.S14P21A205.game.news.dto.MenuMentionCount;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiNewsGenerator {

    private static final Logger log = LoggerFactory.getLogger(AiNewsGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> STYLES = List.of(
            "현장 르포: 오감 묘사로 현장감 있게",
            "인터뷰: 관계자 2~3명 코멘트 활용",
            "분석: 차분하고 권위 있는 해설 톤",
            "속보: 짧고 긴박한 문장 위주"
    );

    private static final String SYSTEM_PROMPT =
            "당신은 '버블팝업' 음식 게임 세계관의 기자입니다. "
            + "출력 규칙: 반드시 아래 형식의 순수 JSON 한 줄만 출력하세요. "
            + "{\"title\":\"제목\",\"content\":\"본문\"} "
            + "이것 외에 다른 텍스트, 코드블록(```), 설명, 수정본, 대안 등 일체 금지. "
            + "title과 content 키 이름을 반드시 포함해야 합니다. "
            + "제목: 한국어 15~25자. '무엇이 어떻게 됐다' 형태로 구체적 작성. 메뉴명이나 지역명 등 고유명사 반드시 포함. "
            + "예시: '강남 타코 매장 급증, 원재료비 상승 우려', '홍대 버블티 열풍 속 줄서기 행렬'. "
            + "본문: 한국어 300~400자. "
            + "언어 규칙: 반드시 순수 한국어만 사용. "
            + "영어·중국어·일본어·한자·알파벳·외국어 한 글자도 절대 포함 금지. "
            + "SNS는 '온라인', hashtag는 '꼬리표', trend는 '유행', resurgence는 '재부상'으로 대체. "
            + "숫자를 직접 쓰지 말고 간접 표현으로 암시. "
            + "괄호(), **, #, 이모지 사용 금지.";

    private final RestClient restClient;
    private final String model;

    public AiNewsGenerator(
            @Value("${GMS_BASE_URL:https://gms.ssafy.io/gmsapi}") String baseUrl,
            @Value("${GMS_KEY:}") String apiKey,
            @Value("${GMS_MODEL:gpt-4.1-nano}") String model) {
        this.model = model;
        log.info("GMS AI base-url={}, model={}", baseUrl, model);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(300));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private String getRandomStyle() {
        return STYLES.get(ThreadLocalRandom.current().nextInt(STYLES.size()));
    }

    // ---- Trend News ----

    public NewsGenerationResult generateTrendNews(long seasonId, int day, List<MenuMentionCount> rankedMenus) {
        String style = getRandomStyle();
        String rankingText = IntStream.range(0, rankedMenus.size())
                .mapToObj(i -> (i + 1) + "위 " + rankedMenus.get(i).menuName())
                .collect(Collectors.joining(", "));

        String prompt = "인기 메뉴 순서: %s. 어떤 메뉴가 뜨는지 자연스럽게 암시하는 트렌드 뉴스 작성. 순위나 등수를 직접 언급하지 말고 인기도를 간접적으로 표현. 상위 메뉴 모두 언급. 본문 300~400자. 스타일: %s"
                .formatted(rankingText, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI trend news failed day {}", day, e);
            return fallbackTrendNews(day, rankedMenus);
        }
    }

    private NewsGenerationResult fallbackTrendNews(int day, List<MenuMentionCount> rankedMenus) {
        String topMenu = rankedMenus.isEmpty() ? "음식" : rankedMenus.get(0).menuName();
        return new NewsGenerationResult(
                "거리에서 감지된 새로운 미식 트렌드",
                "최근 버블팝업 상권 곳곳에서 " + topMenu + "을(를) 찾는 발걸음이 부쩍 늘었다는 소식이 들려오고 있다. "
                + "업계 관계자들은 \"SNS에서의 반응이 심상치 않다\"며 주목하고 있으며, "
                + "일부 점주들은 이미 관련 메뉴 도입을 검토 중인 것으로 알려졌다.");
    }

    // ---- Menu Entry News ----

    public NewsGenerationResult generateMenuEntryNews(long seasonId, int day, List<Map<String, Object>> ranking) {
        String style = getRandomStyle();
        String rankingText = ranking.stream()
                .map(item -> item.get("name") + " " + item.get("storeCount") + "개")
                .collect(Collectors.joining(", "));

        String prompt = "메뉴별 매장수: %s. 매장 많은 메뉴는 원재료비 상승, 적은 메뉴는 틈새 기회. 상위·하위 대비하여 기사 작성. 본문 400~500자. 스타일: %s"
                .formatted(rankingText, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI menu entry news failed day {}", day, e);
            String topMenu = ranking.isEmpty() ? "음식" : (String) ranking.get(0).get("name");
            return new NewsGenerationResult(
                    topMenu + " 원재료 수급에 빨간불?",
                    "최근 " + topMenu + " 관련 매장이 빠르게 늘어나면서 도매시장에서는 원재료 수급에 대한 우려의 목소리가 나오고 있다. "
                    + "한 유통업계 관계자는 \"주문량이 갑자기 늘었다\"며 가격 인상 가능성을 시사했다.");
        }
    }

    // ---- Area Entry News ----

    public NewsGenerationResult generateAreaEntryNews(long seasonId, int day, List<Map<String, Object>> ranking) {
        String style = getRandomStyle();
        String rankingText = ranking.stream()
                .map(item -> item.get("name") + " " + item.get("storeCount") + "개")
                .collect(Collectors.joining(", "));

        String prompt = "지역별 매장수: %s. 밀집 지역은 임대료 폭등, 한산한 지역은 안정. 상위·하위 대비하여 기사 작성. 본문 400~500자. 스타일: %s"
                .formatted(rankingText, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI area entry news failed day {}", day, e);
            String topArea = ranking.isEmpty() ? "지역" : (String) ranking.get(0).get("name");
            return new NewsGenerationResult(
                    topArea + " 상권, 과열 조짐 감지",
                    topArea + " 일대에 팝업 매장이 빠르게 늘어나면서 상가 임대 시장이 들썩이고 있다. "
                    + "인근 중개업소 관계자는 \"문의가 하루에도 수십 건\"이라며 임대료 인상 가능성을 시사했다.");
        }
    }

    // ---- Event Preview News ----

    public NewsGenerationResult generateEventPreviewNews(long seasonId, int day, List<Map<String, Object>> eventData) {
        String style = getRandomStyle();
        String festivalName = (String) eventData.get(0).get("festivalName");
        String locationName = (String) eventData.get(0).get("locationName");

        String prompt;
        if (locationName != null && !locationName.isEmpty()) {
            prompt = "내일 '%s' 지역에서 '%s' 축제 개최. 축제 지역과 축제명을 명확히 언급하고 유동인구·매출 증가 기대감을 담은 예고 기사 작성. 스타일: %s"
                    .formatted(locationName, festivalName, style);
        } else {
            prompt = "내일 '%s' 축제 개최. 축제명을 명확히 언급하고 유동인구·매출 증가 기대감을 담은 예고 기사 작성. 스타일: %s"
                    .formatted(festivalName, style);
        }
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI event preview news failed day {}", day, e);
            return new NewsGenerationResult(
                    "'" + festivalName + "' 개최 소식에 상권 들썩",
                    "내일 '" + festivalName + "' 행사가 열린다는 소식이 전해지면서 상권가에 기대감이 퍼지고 있다. "
                    + "발 빠른 점주들은 이미 손님맞이 준비에 나선 것으로 보인다.");
        }
    }

    // ---- Top Store News ----

    public NewsGenerationResult generateTopStoreNews(long seasonId, int day, String storeName, String menuName,
            int revenue, int salesCount) {
        String style = getRandomStyle();
        String prompt = "오늘 매출 1위 매장: %s, 메뉴: %s, 매출 %d원, %d개 판매. 사장님 가상 인터뷰 포함. 숫자 직접 쓰지 말 것. 스타일: %s"
                .formatted(storeName, menuName, revenue, salesCount, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI top store news failed day {}", day, e);
            return new NewsGenerationResult(
                    "'" + storeName + "' 앞 긴 줄… 무슨 일이?",
                    "오늘 " + storeName + " 앞에는 아침부터 긴 줄이 늘어섰다. " + menuName + "을(를) 맛보려는 손님들로 "
                    + "매장은 문전성시를 이뤘고, 인근 점주들 사이에서도 화제가 됐다.");
        }
    }

    // ---- Cumulative Sales News ----

    public NewsGenerationResult generateCumulativeSalesNews(long seasonId, int day, String storeName, String menuName,
            long totalSales, int milestone) {
        String style = getRandomStyle();
        String prompt = "'%s' 매장 %s 누적 %d개 판매 달성(마일스톤: %d개). 단골 손님 가상 코멘트 포함. 숫자 직접 쓰지 말 것. 스타일: %s"
                .formatted(storeName, menuName, totalSales, milestone, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI cumulative sales news failed day {}", day, e);
            return new NewsGenerationResult(
                    "'" + storeName + "', 놀라운 판매 기록 달성",
                    storeName + "이(가) 조용히 놀라운 기록을 세웠다. " + menuName + " 누적 판매량이 업계에서도 보기 드문 수준에 도달한 것이다. "
                    + "같은 메뉴를 취급하는 인근 매장들도 이 소식에 촉각을 곤두세우고 있다.");
        }
    }

    // ---- Migration News ----

    public NewsGenerationResult generateMigrationNews(long seasonId, int day, List<Map<String, Object>> changes) {
        String style = getRandomStyle();
        String changesText = changes.stream()
                .map(item -> {
                    long change = ((Number) item.get("change")).longValue();
                    return item.get("name") + (change > 0 ? "+" : "") + change;
                })
                .collect(Collectors.joining(", "));

        String prompt = "지역별 매장 이동: %s (양수=유입, 음수=유출). 유입 지역은 경쟁 심화, 유출 지역은 기회. 스타일: %s"
                .formatted(changesText, style);
        try {
            return callAi(prompt);
        } catch (Exception e) {
            log.error("AI migration news failed day {}", day, e);
            return new NewsGenerationResult(
                    "팝업 매장 대이동, 상권 판도 바뀌나",
                    "버블팝업 상권에 지각변동이 일어나고 있다. 일부 지역에서는 팝업 매장이 빠르게 빠져나가는 반면, "
                    + "특정 지역으로 점주들이 대거 몰리는 양상이 포착됐다.");
        }
    }

    // ---- Common AI call ----

    private NewsGenerationResult callAi(String promptText) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1024);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", promptText)));

        long startTime = System.currentTimeMillis();

        String responseJson = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        long elapsed = System.currentTimeMillis() - startTime;

        JsonNode root = MAPPER.readTree(responseJson);
        String text = root.get("choices").get(0).get("message").get("content").asText();

        JsonNode usage = root.get("usage");
        if (usage != null) {
            int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
            int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
            int totalTokens = promptTokens + completionTokens;
            log.info("[AI] model={} | {}ms | in={} out={} total={} | speed={} tok/s",
                    model, elapsed, promptTokens, completionTokens, totalTokens,
                    completionTokens > 0 ? String.format("%.1f", completionTokens * 1000.0 / elapsed) : "N/A");
        } else {
            log.info("[AI] model={} | {}ms", model, elapsed);
        }

        return parseResponse(text);
    }

    private NewsGenerationResult parseResponse(String text) {
        String jsonStr = text.trim();

        // ```json ... ``` 블록 제거
        if (jsonStr.contains("```")) {
            jsonStr = jsonStr.replaceAll("(?s)```json?\\s*", "").replaceAll("(?s)```", "").trim();
        }

        // 이중 이스케이프 정리: "" → "
        if (jsonStr.contains("\"\"")) {
            jsonStr = jsonStr.replace("\"\"", "\"");
        }

        // 키 누락 패턴 복원: {:"val1", :"val2"} → {"title":"val1","content":"val2"}
        if (jsonStr.matches("(?s)\\{\\s*:.*")) {
            jsonStr = jsonStr.replaceFirst("\\{\\s*:", "{\"title\":");
            jsonStr = jsonStr.replaceFirst(",\\s*:", ",\"content\":");
        }

        // 중첩 브레이스 카운팅으로 첫 번째 완전한 JSON 객체 추출
        int braceStart = jsonStr.indexOf('{');
        if (braceStart >= 0) {
            int depth = 0;
            int braceEnd = -1;
            boolean inString = false;
            boolean escaped = false;
            for (int i = braceStart; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) continue;
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        braceEnd = i;
                        break;
                    }
                }
            }
            if (braceEnd > braceStart) {
                jsonStr = jsonStr.substring(braceStart, braceEnd + 1);
            }
        }

        // 1차 파싱
        NewsGenerationResult result = tryParseJson(jsonStr);
        if (result != null) return result;

        // 이스케이프 문자 정리 후 재시도
        String cleaned = jsonStr.replace("\\n", " ").replace("\\\"", "\"");
        result = tryParseJson(cleaned);
        if (result != null) return result;

        // 정규식으로 title/content 추출 시도
        result = tryRegexExtract(text);
        if (result != null) return result;

        // JSON 파싱 실패 시: 텍스트 자체를 content로 사용
        log.warn("Using raw text as news content (first 100 chars): {}",
                text.length() > 100 ? text.substring(0, 100) + "..." : text);
        String title = text.length() > 50 ? text.substring(0, 50) : text;
        title = title.replaceAll("[\\r\\n]", " ").trim();
        return new NewsGenerationResult(sanitize(title), sanitize(text));
    }

    private NewsGenerationResult tryParseJson(String jsonStr) {
        try {
            JsonNode node = MAPPER.readTree(jsonStr);
            if (node.has("title") && node.has("content")) {
                String title = sanitize(node.get("title").asText().strip());
                String content = sanitize(node.get("content").asText().strip());
                return new NewsGenerationResult(title, content);
            }
        } catch (Exception e) {
            log.warn("JSON parse failed: {}", e.getMessage());
        }
        return null;
    }

    /** 정규식으로 title/content 추출 시도 */
    private NewsGenerationResult tryRegexExtract(String text) {
        // "title" 또는 "제목" 키 뒤의 값 추출
        java.util.regex.Matcher titleMatcher = java.util.regex.Pattern
                .compile("(?:\"title\"|\"제목\")\\s*:\\s*\"([^\"]+)\"")
                .matcher(text);
        java.util.regex.Matcher contentMatcher = java.util.regex.Pattern
                .compile("(?:\"content\"|\"본문\")\\s*:\\s*\"([^\"]{50,})\"")
                .matcher(text);
        if (titleMatcher.find() && contentMatcher.find()) {
            return new NewsGenerationResult(
                    sanitize(titleMatcher.group(1)),
                    sanitize(contentMatcher.group(1)));
        }
        return null;
    }

    /** 영어·중국어·특수문자 제거 후처리 */
    private String sanitize(String text) {
        // 영어 단어 제거
        text = text.replaceAll("[a-zA-Z]+", "");
        // 중국어·일본어 문자 제거
        text = text.replaceAll("[\\u4e00-\\u9fff\\u3040-\\u309f\\u30a0-\\u30ff]+", "");
        // 백슬래시 이스케이프 정리
        text = text.replace("\\n", " ").replace("\\\"", "\"");
        // 연속 공백 정리
        text = text.replaceAll("\\s{2,}", " ");
        // 빈 따옴표·괄호 정리
        text = text.replace("''", "").replace("\"\"", "").replace("()", "");
        return text.strip();
    }

    public record NewsGenerationResult(String title, String content) {
    }
}
