package com.ssafy.S14P21A205.game.day.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.day.service.DayClosingJobService;
import com.ssafy.S14P21A205.game.day.service.SeasonDayClosingService;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeasonDayClosingRetrySchedulerTests {

    private final DayClosingJobService jobService =
            org.mockito.Mockito.mock(DayClosingJobService.class);
    private final SeasonDayClosingService closingService =
            org.mockito.Mockito.mock(SeasonDayClosingService.class);

    @Test
    void retryPendingClosingsExecutesEveryDueTarget() {
        when(jobService.findRetryTargets()).thenReturn(List.of(
                new DayClosingJobService.RetryTarget(9L, 3),
                new DayClosingJobService.RetryTarget(9L, 4)
        ));
        SeasonDayClosingRetryScheduler scheduler =
                new SeasonDayClosingRetryScheduler(jobService, closingService);

        scheduler.retryPendingClosings();

        verify(closingService).retryBusinessEnd(9L, 3);
        verify(closingService).retryBusinessEnd(9L, 4);
    }
}
