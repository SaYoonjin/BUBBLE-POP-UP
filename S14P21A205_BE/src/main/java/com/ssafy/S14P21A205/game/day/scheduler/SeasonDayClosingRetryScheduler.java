package com.ssafy.S14P21A205.game.day.scheduler;

import com.ssafy.S14P21A205.game.day.service.DayClosingJobService;
import com.ssafy.S14P21A205.game.day.service.SeasonDayClosingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeasonDayClosingRetryScheduler {

    private final DayClosingJobService dayClosingJobService;
    private final SeasonDayClosingService seasonDayClosingService;

    @Scheduled(
            fixedDelayString = "${app.game.day-closing.retry-interval-ms:10000}",
            initialDelayString = "${app.game.day-closing.retry-initial-delay-ms:10000}"
    )
    public void retryPendingClosings() {
        for (DayClosingJobService.RetryTarget target : dayClosingJobService.findRetryTargets()) {
            try {
                seasonDayClosingService.retryBusinessEnd(target.seasonId(), target.day());
            } catch (Exception e) {
                log.warn(
                        "Day closing retry failed. seasonId={} day={}",
                        target.seasonId(),
                        target.day(),
                        e
                );
            }
        }
    }
}
