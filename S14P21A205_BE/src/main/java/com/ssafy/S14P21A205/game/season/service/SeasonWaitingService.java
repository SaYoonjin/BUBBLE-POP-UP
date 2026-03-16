package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.game.season.dto.GameWaitingResponse;
import com.ssafy.S14P21A205.game.season.dto.GameWaitingStatus;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonWaitingService {

    private final SeasonRepository seasonRepository;

    private Clock clock = Clock.systemDefaultZone();

    public GameWaitingResponse getWaitingStatus() {
        Season inProgressSeason = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)
                .orElse(null);
        if (inProgressSeason != null) {
            return new GameWaitingResponse(
                    GameWaitingStatus.IN_PROGRESS,
                    null,
                    resolveCurrentDay(inProgressSeason),
                    null
            );
        }

        Season scheduledSeason = seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED)
                .orElse(null);
        if (scheduledSeason != null) {
            return new GameWaitingResponse(
                    GameWaitingStatus.WAITING,
                    resolveSeasonNumber(scheduledSeason.getId()),
                    null,
                    resolveRemainingMinutes(scheduledSeason.getStartTime())
            );
        }

        Integer nextSeasonNumber = seasonRepository.findFirstByOrderByIdDesc()
                .map(Season::getId)
                .map(seasonId -> Math.toIntExact(seasonId + 1))
                .orElse(1);

        return new GameWaitingResponse(
                GameWaitingStatus.WAITING,
                nextSeasonNumber,
                null,
                null
        );
    }

    private Integer resolveCurrentDay(Season season) {
        Integer currentDay = season.getCurrentDay();
        if (currentDay == null || currentDay < 1) {
            return 1;
        }
        if (season.getTotalDays() != null && currentDay > season.getTotalDays()) {
            return season.getTotalDays();
        }
        return currentDay;
    }

    private Integer resolveRemainingMinutes(LocalDateTime startTime) {
        if (startTime == null) {
            return null;
        }
        long remainingMinutes = Duration.between(LocalDateTime.now(clock), startTime).toMinutes();
        return Math.toIntExact(Math.max(remainingMinutes, 0L));
    }

    private Integer resolveSeasonNumber(Long seasonId) {
        return seasonId == null ? null : Math.toIntExact(seasonId);
    }
}
