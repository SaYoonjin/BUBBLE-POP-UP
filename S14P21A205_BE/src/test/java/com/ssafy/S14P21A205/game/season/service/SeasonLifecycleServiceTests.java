package com.ssafy.S14P21A205.game.season.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import java.time.Clock;
import java.time.Instant;
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
    private SeasonFinalRankingService seasonFinalRankingService;

    private SeasonLifecycleService seasonLifecycleService;

    @BeforeEach
    void setUp() {
        seasonLifecycleService = new SeasonLifecycleService(seasonRepository, seasonFinalRankingService);
    }

    @Test
    void synchronizeStartsScheduledSeasonWithoutSchedulingNextSeason() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        LocalDateTime now = seasonStartAt.plusSeconds(120);
        Season scheduledSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));

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
        assertThat(scheduledSeason.getEndTime()).isEqualTo(seasonStartAt.plusSeconds(1800));
        verify(seasonRepository, never()).save(any(Season.class));
        verify(seasonFinalRankingService, never()).saveFinalRankings(any(Season.class));
    }

    @Test
    void synchronizeFinishesInProgressSeasonCreatesNextSeasonAndSavesFinalRankings() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        LocalDateTime now = seasonStartAt.plusSeconds(1800);
        Season inProgressSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        inProgressSeason.start();

        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(Instant.from(now.atZone(ZoneId.of("Asia/Seoul"))), ZoneId.of("Asia/Seoul"))
        );

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgressSeason));
        when(seasonRepository.existsByStatusAndStartTime(
                eq(SeasonStatus.SCHEDULED),
                eq(seasonStartAt.plusSeconds(1800))
        )).thenReturn(false);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seasonLifecycleService.synchronize();

        assertThat(inProgressSeason.getStatus()).isEqualTo(SeasonStatus.FINISHED);
        assertThat(inProgressSeason.getCurrentDay()).isEqualTo(7);
        assertThat(inProgressSeason.getEndTime()).isEqualTo(seasonStartAt.plusSeconds(1800));
        verify(seasonFinalRankingService).saveFinalRankings(inProgressSeason);

        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(seasonCaptor.capture());
        Season nextSeason = seasonCaptor.getValue();
        assertThat(nextSeason.getStatus()).isEqualTo(SeasonStatus.SCHEDULED);
        assertThat(nextSeason.getStartTime()).isEqualTo(seasonStartAt.plusSeconds(1800));
        assertThat(nextSeason.getEndTime()).isEqualTo(seasonStartAt.plusSeconds(3600));
        assertThat(nextSeason.getTotalDays()).isEqualTo(7);
    }
}