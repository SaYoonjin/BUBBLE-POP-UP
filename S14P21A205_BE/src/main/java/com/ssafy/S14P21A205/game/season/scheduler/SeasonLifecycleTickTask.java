package com.ssafy.S14P21A205.game.season.scheduler;

import com.ssafy.S14P21A205.game.season.service.SeasonLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeasonLifecycleTickTask {

    private final SeasonLifecycleService seasonLifecycleService;

    @Scheduled(fixedRateString = "${app.game.season-lifecycle.fixed-rate-ms:10000}")
    public void synchronizeSeasonLifecycle() {
        // 1) Spark ETL + 뉴스 생성 (트랜잭션 밖 — Spark TRUNCATE DDL 충돌 방지)
        seasonLifecycleService.prepareScheduledSeasonIfNeeded();
        // 2) 시즌 상태 전환 + rebuild (트랜잭션 안)
        seasonLifecycleService.synchronize();
    }
}