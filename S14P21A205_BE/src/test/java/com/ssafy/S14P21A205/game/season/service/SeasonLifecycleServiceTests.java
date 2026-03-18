package com.ssafy.S14P21A205.game.season.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.day.scheduler.SeasonDayClosingScheduler;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeasonLifecycleServiceTests {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private SeasonDayClosingScheduler seasonDayClosingScheduler;

    private SeasonLifecycleService seasonLifecycleService;
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    @BeforeEach
    void setUp() {
        seasonLifecycleService = new SeasonLifecycleService(seasonRepository, seasonDayClosingScheduler);
    }

    @Test
    void synchronizeStartsScheduledSeasonWhenStartTimeHasArrived() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        LocalDateTime now = seasonStartAt.plusSeconds(1);
        Season scheduledSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        ReflectionTestUtils.setField(scheduledSeason, "id", 11L);

        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        );

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED))
                .thenReturn(Optional.of(scheduledSeason));

        seasonLifecycleService.synchronize();

        assertThat(scheduledSeason.getStatus()).isEqualTo(SeasonStatus.IN_PROGRESS);
        assertThat(scheduledSeason.getCurrentDay()).isEqualTo(1);
        assertThat(scheduledSeason.getEndTime()).isEqualTo(seasonTimelineService.resolveNextSeasonStartAt(scheduledSeason));
        verify(seasonDayClosingScheduler).synchronize(scheduledSeason);
        verify(seasonRepository, never()).save(any(Season.class));
    }

    @Test
    void synchronizeUpdatesCurrentDayForInProgressSeason() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        Season inProgressSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        ReflectionTestUtils.setField(inProgressSeason, "id", 21L);
        inProgressSeason.start();

        LocalDateTime now = seasonTimelineService.day(inProgressSeason, 2).businessStart().plusSeconds(1);
        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        );

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(inProgressSeason));

        seasonLifecycleService.synchronize();

        assertThat(inProgressSeason.getStatus()).isEqualTo(SeasonStatus.IN_PROGRESS);
        assertThat(inProgressSeason.getCurrentDay()).isEqualTo(2);
        verify(seasonDayClosingScheduler).synchronize(inProgressSeason);
        verify(seasonDayClosingScheduler, never()).clear(21L);
    }

    @Test
    void synchronizeFinishesSeasonAndSchedulesNextSeasonWhenSeasonHasEnded() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        Season inProgressSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        ReflectionTestUtils.setField(inProgressSeason, "id", 31L);
        inProgressSeason.start();

        LocalDateTime seasonEndAt = seasonTimelineService.resolveNextSeasonStartAt(inProgressSeason);
        LocalDateTime now = seasonEndAt.plusSeconds(1);
        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        );

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(inProgressSeason));
        when(seasonRepository.existsByStatusAndStartTime(SeasonStatus.SCHEDULED, seasonEndAt)).thenReturn(false);

        seasonLifecycleService.synchronize();

        assertThat(inProgressSeason.getStatus()).isEqualTo(SeasonStatus.FINISHED);
        assertThat(inProgressSeason.getCurrentDay()).isEqualTo(7);
        verify(seasonDayClosingScheduler).synchronize(inProgressSeason);
        verify(seasonDayClosingScheduler).clear(31L);

        ArgumentCaptor<Season> nextSeasonCaptor = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(nextSeasonCaptor.capture());
        Season nextSeason = nextSeasonCaptor.getValue();
        assertThat(nextSeason.getStatus()).isEqualTo(SeasonStatus.SCHEDULED);
        assertThat(nextSeason.getTotalDays()).isEqualTo(7);
        assertThat(nextSeason.getStartTime()).isEqualTo(seasonEndAt);
        assertThat(nextSeason.getEndTime()).isEqualTo(seasonEndAt.plus(seasonTimelineService.seasonCycleDuration(7)));
    }
}
