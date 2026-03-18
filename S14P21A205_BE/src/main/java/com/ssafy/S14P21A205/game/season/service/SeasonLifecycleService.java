package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.time.model.SeasonTimePoint;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SeasonLifecycleService {

    private final SeasonRepository seasonRepository;
    private final SeasonFinalRankingService seasonFinalRankingService;

    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    private Clock clock = Clock.systemDefaultZone();

    public void synchronize() {
        LocalDateTime now = LocalDateTime.now(clock);

        Season inProgressSeason = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS).orElse(null);
        if (inProgressSeason != null) {
            synchronizeInProgressSeason(inProgressSeason, now);
            return;
        }

        Season scheduledSeason = seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED).orElse(null);
        if (scheduledSeason == null || scheduledSeason.getStartTime() == null || scheduledSeason.getStartTime().isAfter(now)) {
            return;
        }

        scheduledSeason.start();
        scheduledSeason.updateEndTime(resolveSeasonEndAt(scheduledSeason));
        synchronizeInProgressSeason(scheduledSeason, now);
    }

    private void synchronizeInProgressSeason(Season season, LocalDateTime now) {
        LocalDateTime seasonEndAt = resolveSeasonEndAt(season);
        season.updateEndTime(seasonEndAt);

        SeasonTimePoint timePoint = seasonTimelineService.resolve(season, now);
        log.info(
                "season-timeline seasonId={} now={} phase={} day={} gameTime={} tick={} remaining={} joinEnabled={} playableFromDay={} seasonEndAt={}",
                season.getId(),
                now,
                timePoint.phase(),
                timePoint.currentDay(),
                timePoint.gameTime(),
                timePoint.tick(),
                timePoint.remainingPhaseSeconds(),
                timePoint.joinEnabled(),
                timePoint.joinPlayableFromDay(),
                seasonEndAt
        );
        if (timePoint.currentDay() != null) {
            season.syncCurrentDay(timePoint.currentDay());
        }

        if (!now.isBefore(seasonEndAt)) {
            season.finish();
            seasonFinalRankingService.saveFinalRankings(season);
            scheduleNextSeasonIfNeeded(season, seasonEndAt);
        }
    }

    private LocalDateTime resolveSeasonEndAt(Season season) {
        return seasonTimelineService.resolveNextSeasonStartAt(season);
    }

    private void scheduleNextSeasonIfNeeded(Season finishedSeason, LocalDateTime nextSeasonStartAt) {
        if (seasonRepository.existsByStatusAndStartTime(SeasonStatus.SCHEDULED, nextSeasonStartAt)) {
            return;
        }

        int totalDays = finishedSeason.getTotalDays() == null || finishedSeason.getTotalDays() <= 0
                ? 7
                : finishedSeason.getTotalDays();
        LocalDateTime nextSeasonEndAt = nextSeasonStartAt.plus(seasonTimelineService.seasonCycleDuration(totalDays));
        seasonRepository.save(Season.createScheduled(totalDays, nextSeasonStartAt, nextSeasonEndAt));
    }
}