package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.game.day.entity.DayClosingJob;
import com.ssafy.S14P21A205.game.day.entity.DayClosingJobStatus;
import com.ssafy.S14P21A205.game.day.repository.DayClosingJobRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DayClosingJobService {

    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(1);
    private static final long BASE_RETRY_SECONDS = 10L;
    private static final long MAX_RETRY_SECONDS = 300L;

    private final DayClosingJobRepository dayClosingJobRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerIfMissing(Season season, int day) {
        if (dayClosingJobRepository.existsBySeason_IdAndDay(season.getId(), day)) {
            return;
        }
        dayClosingJobRepository.saveAndFlush(
                DayClosingJob.pending(season, day, LocalDateTime.now(clock))
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long seasonId, int day) {
        LocalDateTime now = LocalDateTime.now(clock);
        DayClosingJob job = dayClosingJobRepository.findBySeasonIdAndDayForUpdate(seasonId, day)
                .orElse(null);
        if (job == null || !job.isClaimable(now)) {
            return false;
        }
        job.start(now.plus(PROCESSING_LEASE));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long seasonId, int day) {
        DayClosingJob job = dayClosingJobRepository.findBySeasonIdAndDayForUpdate(seasonId, day)
                .orElseThrow(() -> new IllegalStateException("Day closing job not found."));
        job.complete(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleRetry(Long seasonId, int day, Throwable failure) {
        DayClosingJob job = dayClosingJobRepository.findBySeasonIdAndDayForUpdate(seasonId, day)
                .orElseThrow(() -> new IllegalStateException("Day closing job not found."));
        int retryCount = job.getRetryCount() == null ? 0 : job.getRetryCount();
        long delaySeconds = Math.min(
                MAX_RETRY_SECONDS,
                BASE_RETRY_SECONDS * (1L << Math.min(retryCount, 5))
        );
        job.scheduleRetry(
                LocalDateTime.now(clock).plusSeconds(delaySeconds),
                summarize(failure)
        );
    }

    @Transactional(readOnly = true)
    public List<RetryTarget> findRetryTargets() {
        return dayClosingJobRepository
                .findTop50ByStatusNotAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        DayClosingJobStatus.COMPLETED,
                        LocalDateTime.now(clock)
                )
                .stream()
                .map(job -> new RetryTarget(job.getSeason().getId(), job.getDay()))
                .toList();
    }

    private String summarize(Throwable failure) {
        if (failure == null) {
            return "Unknown day closing failure";
        }
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    public record RetryTarget(Long seasonId, int day) {
    }
}
