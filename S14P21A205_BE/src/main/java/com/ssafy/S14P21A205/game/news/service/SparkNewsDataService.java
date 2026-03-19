package com.ssafy.S14P21A205.game.news.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.S14P21A205.game.news.dto.MenuMentionCount;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SparkNewsDataService {

    private static final Logger log = LoggerFactory.getLogger(SparkNewsDataService.class);
    private static final String CONTAINER_NAME = "spark-master";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void runNewsEtl() {
        submitSparkJob("etl_news_score.py");
    }

    public Map<Integer, List<MenuMentionCount>> getMenuMentionsForDays(int totalDays) {
        String json = readNewsMentions(totalDays);
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, List<Map<String, Object>>> raw = MAPPER.readValue(
                    json, new TypeReference<>() {});

            return raw.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> Integer.parseInt(e.getKey()),
                            e -> e.getValue().stream()
                                    .map(m -> new MenuMentionCount(
                                            (String) m.get("menuName"),
                                            ((Number) m.get("mentionCount")).longValue()))
                                    .toList()));
        } catch (Exception e) {
            log.error("Failed to parse news mentions JSON", e);
            return Map.of();
        }
    }

    private String readNewsMentions(int totalDays) {
        List<String> command = List.of(
                "docker", "exec", CONTAINER_NAME,
                "/spark/bin/spark-submit",
                "--master", "spark://spark-master:7077",
                "/opt/spark-jobs/read_news_mentions.py",
                String.valueOf(totalDays));

        log.info("[SPARK] Starting read_news_mentions.py (totalDays={})", totalDays);
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            // stdout와 stderr를 별도 스레드에서 병렬 소비
            Thread stdoutThread = new Thread(() -> {
                try (var in = process.getInputStream()) {
                    in.transferTo(stdout);
                } catch (Exception ignored) {}
            });
            stdoutThread.setDaemon(true);
            stdoutThread.start();
            Thread stderrThread = new Thread(() -> {
                try (var err = process.getErrorStream()) {
                    err.transferTo(java.io.OutputStream.nullOutputStream());
                } catch (Exception ignored) {}
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            boolean finished = process.waitFor(3, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                stdoutThread.interrupt();
                stderrThread.interrupt();
                log.error("[SPARK] read_news_mentions.py timed out (3min)");
                return null;
            }

            stdoutThread.join(5000);
            stderrThread.join(5000);

            String output = stdout.toString(StandardCharsets.UTF_8).trim();
            String[] lines = output.split("\n");
            log.info("[SPARK] read_news_mentions.py completed (exit={}, outputLines={})",
                    process.exitValue(), lines.length);
            return lines[lines.length - 1];
        } catch (Exception e) {
            log.error("[SPARK] Failed to read news mentions", e);
            return null;
        }
    }

    private void submitSparkJob(String scriptName) {
        List<String> command = List.of(
                "docker", "exec", CONTAINER_NAME,
                "/spark/bin/spark-submit",
                "--master", "spark://spark-master:7077",
                "/opt/spark-jobs/" + scriptName);

        log.info("[SPARK] Starting job: {}", scriptName);
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // stdout를 별도 스레드에서 소비하여 블로킹 방지
            Thread drainThread = new Thread(() -> {
                try (var in = process.getInputStream()) {
                    in.transferTo(java.io.OutputStream.nullOutputStream());
                } catch (Exception ignored) {}
            });
            drainThread.setDaemon(true);
            drainThread.start();

            boolean finished = process.waitFor(3, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                drainThread.interrupt();
                log.error("[SPARK] Job timed out (3min): {}", scriptName);
            } else {
                drainThread.join(5000);
                log.info("[SPARK] Job completed: {} (exit={})", scriptName, process.exitValue());
            }
        } catch (Exception e) {
            log.error("[SPARK] Job failed: {}", scriptName, e);
        }
    }
}
