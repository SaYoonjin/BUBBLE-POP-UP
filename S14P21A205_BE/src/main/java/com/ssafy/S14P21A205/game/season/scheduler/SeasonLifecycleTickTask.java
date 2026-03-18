package com.ssafy.S14P21A205.game.season.scheduler;

import com.ssafy.S14P21A205.game.season.service.SeasonLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonLifecycleTickTask {

    private final SeasonLifecycleService seasonLifecycleService;

    @Scheduled(fixedRateString = "${app.game.season-lifecycle.fixed-rate-ms:10000}")
    @Transactional
    public void synchronizeSeasonLifecycle() {
        seasonLifecycleService.synchronize();
    }
}