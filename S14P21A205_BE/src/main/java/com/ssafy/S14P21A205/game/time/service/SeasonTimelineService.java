package com.ssafy.S14P21A205.game.time.service;

import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import java.time.Duration;
import java.time.LocalDateTime;

public class SeasonTimelineService {

    private final GameTimePolicy gameTimePolicy = new GameTimePolicy();

    public DayWindow currentDay(LocalDateTime currentDayStart, int currentDay, int totalDays) {
        return gameTimePolicy.currentDay(currentDayStart, currentDay, totalDays);
    }

    public DayWindow day(LocalDateTime currentDayStart, int currentDay, int totalDays, int targetDay) {
        return gameTimePolicy.day(currentDayStart, currentDay, totalDays, targetDay);
    }

    public LocalDateTime resolveAppliedAt(
            LocalDateTime currentDayStart,
            int currentDay,
            int totalDays,
            int appliedDay,
            Integer offsetSeconds
    ) {
        return gameTimePolicy.resolveAppliedAt(currentDayStart, currentDay, totalDays, appliedDay, offsetSeconds);
    }

    public LocalDateTime resolveEndedAt(
            LocalDateTime currentDayStart,
            int currentDay,
            int totalDays,
            int appliedDay,
            Integer expireOffsetSeconds,
            EventEndTime endTime
    ) {
        return gameTimePolicy.resolveEndedAt(
                currentDayStart,
                currentDay,
                totalDays,
                appliedDay,
                expireOffsetSeconds,
                endTime
        );
    }

    public String formatGameTime(Integer offsetSeconds) {
        return gameTimePolicy.formatGameTime(offsetSeconds);
    }

    public Duration businessDuration() {
        return gameTimePolicy.businessDuration();
    }

    public Duration reportDuration() {
        return gameTimePolicy.reportDuration();
    }

    public Duration prepDuration() {
        return gameTimePolicy.prepDuration();
    }

    public Duration dayDuration() {
        return gameTimePolicy.dayDuration();
    }
}
