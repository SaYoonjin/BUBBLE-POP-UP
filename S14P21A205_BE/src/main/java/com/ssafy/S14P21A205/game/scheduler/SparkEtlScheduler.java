package com.ssafy.S14P21A205.game.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SparkEtlScheduler {

    private static final Logger log = LoggerFactory.getLogger(SparkEtlScheduler.class);

    private static final LocalDate START_BOUND = LocalDate.of(2023, 1, 1);
    private static final LocalDate END_BOUND = LocalDate.of(2024, 12, 25);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter BATCH_KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();
    private static final String CONTAINER_NAME = "spark-master";

    @Scheduled(fixedRate = 1800000)
    public void runEtl() {
        String randomDate = pickRandomDate();
        String batchKey = "spark-" + randomDate + "-" + LocalDateTime.now().format(BATCH_KEY_FMT);
        log.info("Spark ETL started. date={}, batchKey={}", randomDate, batchKey);
        submitSparkJob("etl_population_score.py", randomDate, batchKey);
        submitSparkJob("etl_traffic_score.py", randomDate, batchKey);
    }

    private String pickRandomDate() {
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

            try (var in = process.getInputStream()) {
                in.transferTo(java.io.OutputStream.nullOutputStream());
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.error("Spark job failed: {}", scriptName, e);
        }
    }
}
