package com.ssafy.S14P21A205.game.day.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.day.entity.DayClosingJob;
import com.ssafy.S14P21A205.game.day.entity.DayClosingJobStatus;
import com.ssafy.S14P21A205.game.day.repository.DayClosingJobRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DayClosingJobServiceTests {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW_INSTANT = Instant.parse("2026-07-03T01:00:00Z");

    private final DayClosingJobRepository repository =
            org.mockito.Mockito.mock(DayClosingJobRepository.class);
    private final Clock clock = Clock.fixed(NOW_INSTANT, ZONE);

    private DayClosingJobService service;

    @BeforeEach
    void setUp() {
        service = new DayClosingJobService(repository, clock);
    }

    @Test
    void registerIfMissingPersistsPendingJob() {
        Season season = season(9L);
        when(repository.existsBySeason_IdAndDay(9L, 3)).thenReturn(false);

        service.registerIfMissing(season, 3);

        ArgumentCaptor<DayClosingJob> captor = ArgumentCaptor.forClass(DayClosingJob.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DayClosingJobStatus.PENDING);
        assertThat(captor.getValue().getRetryCount()).isZero();
        assertThat(captor.getValue().getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void claimUsesProcessingLeaseToPreventConcurrentExecution() {
        DayClosingJob job = DayClosingJob.pending(season(9L), 3, LocalDateTime.now(clock));
        when(repository.findBySeasonIdAndDayForUpdate(9L, 3)).thenReturn(Optional.of(job));

        assertThat(service.claim(9L, 3)).isTrue();
        assertThat(job.getStatus()).isEqualTo(DayClosingJobStatus.PROCESSING);
        assertThat(job.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock).plusMinutes(1));

        assertThat(service.claim(9L, 3)).isFalse();
    }

    @Test
    void scheduleRetryStoresFailureAndAppliesBackoff() {
        DayClosingJob job = DayClosingJob.pending(season(9L), 3, LocalDateTime.now(clock));
        job.start(LocalDateTime.now(clock).plusMinutes(1));
        when(repository.findBySeasonIdAndDayForUpdate(9L, 3)).thenReturn(Optional.of(job));

        service.scheduleRetry(9L, 3, new IllegalStateException("Redis unavailable"));

        assertThat(job.getStatus()).isEqualTo(DayClosingJobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock).plusSeconds(10));
        assertThat(job.getLastError()).contains("Redis unavailable");
    }

    private Season season(Long id) {
        Season season = org.springframework.beans.BeanUtils.instantiateClass(Season.class);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }
}
