package com.ssafy.S14P21A205.game.time.service;

import com.ssafy.S14P21A205.game.time.model.SeasonPhase;
import com.ssafy.S14P21A205.game.time.model.SeasonTimePoint;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import java.time.LocalDateTime;

public class GameClockService {

    private final GameTimePolicy gameTimePolicy = new GameTimePolicy();

    public SeasonTimePoint resolve(LocalDateTime currentTime, LocalDateTime seasonStartTime, int totalDays) {
        SeasonPhase phase = gameTimePolicy.resolveSeasonPhase(seasonStartTime, totalDays, currentTime);
        LocalDateTime phaseStartAt = gameTimePolicy.resolvePhaseStartAt(seasonStartTime, totalDays, currentTime);
        LocalDateTime phaseEndAt = gameTimePolicy.resolvePhaseEndAt(seasonStartTime, totalDays, currentTime);
        return new SeasonTimePoint(
                currentTime,
                phase,
                gameTimePolicy.resolveCurrentDay(seasonStartTime, totalDays, currentTime),
                phaseStartAt,
                phaseEndAt,
                gameTimePolicy.resolveElapsedPhaseSeconds(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveRemainingPhaseSeconds(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveElapsedBusinessSeconds(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveGameTime(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveTick(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.isJoinEnabled(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveJoinPlayableFromDay(seasonStartTime, totalDays, currentTime),
                gameTimePolicy.resolveNextSeasonStartAt(seasonStartTime, totalDays)
        );
    }
}

