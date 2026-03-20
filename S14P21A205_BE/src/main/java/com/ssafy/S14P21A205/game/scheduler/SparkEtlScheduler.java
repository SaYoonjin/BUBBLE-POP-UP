package com.ssafy.S14P21A205.game.scheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SparkEtlScheduler {

    private static final Logger log = LoggerFactory.getLogger(SparkEtlScheduler.class);

    private static final LocalDate START_BOUND = LocalDate.of(2023, 1, 1);
    private static final LocalDate END_BOUND = LocalDate.of(2024, 12, 25);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter BATCH_KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int REQUIRED_CONSECUTIVE_DAYS = 7;
    private static final Random RANDOM = new Random();
    private static final String CONTAINER_NAME = "spark-master";

    private final Clock clock;

    public void runEtl() {
        String randomDate = pickDateFromHdfs();
        String batchKey = "spark-" + randomDate + "-" + LocalDateTime.now(clock).format(BATCH_KEY_FMT);
        log.info("Spark ETL started. date={}, batchKey={}", randomDate, batchKey);
        submitSparkJob("etl_population_score.py", randomDate, batchKey);
        submitSparkJob("etl_traffic_score.py", randomDate, batchKey);
    }

    /**
     * HDFS에서 가용 날짜를 조회하여 7일 연속 구간의 시작 날짜를 랜덤으로 선택합니다.
     * HDFS 조회 실패 시 하드코딩 범위에서 fallback합니다.
     */
    private String pickDateFromHdfs() {
        List<String> availableDates = fetchAvailableDates();
        if (availableDates.isEmpty()) {
            log.warn("HDFS 가용 날짜 조회 실패. 하드코딩 범위에서 fallback합니다.");
            return pickRandomDateFromRange();
        }

        List<String> validStartDates = findConsecutiveStartDates(availableDates, REQUIRED_CONSECUTIVE_DAYS);
        if (validStartDates.isEmpty()) {
            log.warn("HDFS에 {}일 연속 구간이 없습니다. 가용 날짜 중 첫 번째를 사용합니다. availableDates={}",
                    REQUIRED_CONSECUTIVE_DAYS, availableDates);
            return availableDates.get(0);
        }

        String selected = validStartDates.get(RANDOM.nextInt(validStartDates.size()));
        log.info("HDFS 가용 날짜에서 선택. startDate={}, validStartDates={}", selected, validStartDates.size());
        return selected;
    }

    /**
     * list_available_dates.py를 실행하여 HDFS Parquet의 고유 날짜 목록을 조회합니다.
     */
    private List<String> fetchAvailableDates() {
        List<String> command = List.of(
                "docker", "exec", CONTAINER_NAME,
                "/spark/bin/spark-submit",
                "--master", "spark://spark-master:7077",
                "/opt/spark-jobs/list_available_dates.py"
        );

        List<String> dates = new ArrayList<>();
        List<String> nonDateLines = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("DATE:")) {
                        dates.add(line.substring(5).trim());
                    } else {
                        nonDateLines.add(line);
                    }
                }
            }

            boolean finished = process.waitFor(3, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.warn("list_available_dates.py 타임아웃");
                return List.of();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("list_available_dates.py 비정상 종료. exitCode={}", exitCode);
            }
            if (dates.isEmpty() && !nonDateLines.isEmpty()) {
                int tailSize = Math.min(nonDateLines.size(), 10);
                log.warn("DATE: 라인 없음. 마지막 {}줄: {}", tailSize,
                        nonDateLines.subList(nonDateLines.size() - tailSize, nonDateLines.size()));
            }
        } catch (Exception e) {
            log.error("HDFS 가용 날짜 조회 실패", e);
        }

        log.info("HDFS 가용 날짜 조회 완료. dates={}", dates);
        return dates;
    }

    /**
     * 정렬된 날짜 목록에서 requiredDays일 연속 가능한 시작 날짜를 찾습니다.
     */
    private List<String> findConsecutiveStartDates(List<String> sortedDates, int requiredDays) {
        List<String> validStarts = new ArrayList<>();

        for (int i = 0; i <= sortedDates.size() - requiredDays; i++) {
            LocalDate start = LocalDate.parse(sortedDates.get(i), DATE_FMT);
            LocalDate end = LocalDate.parse(sortedDates.get(i + requiredDays - 1), DATE_FMT);

            if (ChronoUnit.DAYS.between(start, end) == requiredDays - 1) {
                validStarts.add(sortedDates.get(i));
            }
        }

        return validStarts;
    }

    /**
     * 하드코딩 범위에서 랜덤 날짜 선택 (fallback).
     */
    private String pickRandomDateFromRange() {
        long totalDays = ChronoUnit.DAYS.between(START_BOUND, END_BOUND) + 1;
        long randomDayOffset = RANDOM.nextLong(totalDays);
        return START_BOUND.plusDays(randomDayOffset).format(DATE_FMT);
    }

    private void submitSparkJob(String scriptName, String startDate, String batchKey) {
        List<String> command = List.of(
                "docker", "exec", CONTAINER_NAME,
                "/spark/bin/spark-submit",
                "--master", "spark://spark-master:7077",
                "/opt/spark-jobs/" + scriptName,
                startDate,
                batchKey
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<String> tailLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    tailLines.add(line);
                    if (tailLines.size() > 10) {
                        tailLines.remove(0);
                    }
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Spark job 타임아웃: {}", scriptName);
            } else {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    log.error("Spark job 실패: {} exitCode={} tail={}", scriptName, exitCode, tailLines);
                } else {
                    log.info("Spark job 완료: {} tail={}", scriptName, tailLines.subList(Math.max(0, tailLines.size() - 3), tailLines.size()));
                }
            }
        } catch (Exception e) {
            log.error("Spark job 실행 오류: {}", scriptName, e);
        }
    }
}
