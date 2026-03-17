package com.ssafy.S14P21A205.game.time.service;

import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.model.GamePhase;
import com.ssafy.S14P21A205.game.time.model.GameTimePoint;
import java.time.Duration;
import java.time.LocalDateTime;

public class GameClockService {

    public GameTimePoint resolve(LocalDateTime currentTime, DayWindow dayWindow) {
        LocalDateTime occurredAt = clamp(currentTime, dayWindow.dayStart(), dayWindow.reportEnd());
        return new GameTimePoint(
                occurredAt,
                resolvePhase(occurredAt, dayWindow),
                resolveElapsedBusinessSeconds(occurredAt, dayWindow)
        );
    }

    public GamePhase resolvePhase(LocalDateTime currentTime, DayWindow dayWindow) {
        if (currentTime.isBefore(dayWindow.businessStart())) {
            return GamePhase.PREPARING;
        }
        if (currentTime.isBefore(dayWindow.businessEnd())) {
            return GamePhase.BUSINESS;
        }
        if (currentTime.isBefore(dayWindow.reportEnd())) {
            return GamePhase.REPORT;
        }
        return GamePhase.CLOSED;
    }

    private long resolveElapsedBusinessSeconds(LocalDateTime currentTime, DayWindow dayWindow) {
        if (!currentTime.isAfter(dayWindow.businessStart())) {
            return 0L;
        }
        LocalDateTime boundedTime = currentTime.isAfter(dayWindow.businessEnd())
                ? dayWindow.businessEnd()
                : currentTime;
        return Duration.between(dayWindow.businessStart(), boundedTime).toSeconds();
    }

    private LocalDateTime clamp(LocalDateTime currentTime, LocalDateTime lowerBound, LocalDateTime upperBound) {
        if (currentTime.isBefore(lowerBound)) {
            return lowerBound;
        }
        if (currentTime.isAfter(upperBound)) {
            return upperBound;
        }
        return currentTime;
    }
}
