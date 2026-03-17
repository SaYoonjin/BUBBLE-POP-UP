package com.ssafy.S14P21A205.game.time.policy;

import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GameTimePolicy {

    public static final int BUSINESS_OPEN_HOUR = 10;
    public static final int BUSINESS_CLOSE_HOUR = 22;
    public static final int REALTIME_SEGMENT_COUNT = 3;

    private static final Duration PREP_DURATION = Duration.ofSeconds(50);
    private static final Duration BUSINESS_DURATION = Duration.ofMinutes(2);
    private static final Duration REPORT_DURATION = Duration.ofSeconds(10);
    private static final Duration DAY_DURATION = PREP_DURATION.plus(BUSINESS_DURATION).plus(REPORT_DURATION);
    private static final int GAME_BUSINESS_DURATION_MINUTES = (BUSINESS_CLOSE_HOUR - BUSINESS_OPEN_HOUR) * 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public DayWindow currentDay(LocalDateTime currentDayStart, int currentDay, int totalDays) {
        return day(currentDayStart, currentDay, totalDays, currentDay);
    }

    public DayWindow day(LocalDateTime currentDayStart, int currentDay, int totalDays, int targetDay) {
        LocalDateTime targetDayStart = currentDayStart.minus(DAY_DURATION.multipliedBy((long) currentDay - targetDay));
        LocalDateTime businessStart = targetDayStart.plus(PREP_DURATION);
        LocalDateTime businessEnd = businessStart.plus(BUSINESS_DURATION);
        LocalDateTime reportEnd = businessEnd.plus(REPORT_DURATION);
        LocalDateTime dayOneStart = currentDayStart.minus(DAY_DURATION.multipliedBy(currentDay - 1L));
        LocalDateTime seasonPlayableEnd = dayOneStart.plus(DAY_DURATION.multipliedBy(totalDays));
        return new DayWindow(targetDayStart, businessStart, businessEnd, reportEnd, seasonPlayableEnd);
    }

    public LocalDateTime resolveAppliedAt(
            LocalDateTime currentDayStart,
            int currentDay,
            int totalDays,
            int appliedDay,
            Integer offsetSeconds
    ) {
        DayWindow appliedDayWindow = day(currentDayStart, currentDay, totalDays, appliedDay);
        return appliedDayWindow.businessStart().plusSeconds(normalizeOffsetSeconds(offsetSeconds));
    }

    public LocalDateTime resolveEndedAt(
            LocalDateTime currentDayStart,
            int currentDay,
            int totalDays,
            int appliedDay,
            Integer expireOffsetSeconds,
            EventEndTime endTime
    ) {
        DayWindow appliedDayWindow = day(currentDayStart, currentDay, totalDays, appliedDay);
        if (expireOffsetSeconds != null && expireOffsetSeconds >= 0) {
            return appliedDayWindow.businessStart().plusSeconds(expireOffsetSeconds);
        }
        return endTime == EventEndTime.SEASON_END
                ? appliedDayWindow.seasonPlayableEnd()
                : appliedDayWindow.businessEnd();
    }

    public String formatGameTime(Integer offsetSeconds) {
        long boundedOffsetSeconds = Math.max(0L, Math.min(normalizeOffsetSeconds(offsetSeconds), BUSINESS_DURATION.toSeconds()));
        long gameMinutes = boundedOffsetSeconds * GAME_BUSINESS_DURATION_MINUTES / BUSINESS_DURATION.toSeconds();
        return LocalTime.of(BUSINESS_OPEN_HOUR, 0).plusMinutes(gameMinutes).format(TIME_FORMATTER);
    }

    public Duration businessDuration() {
        return BUSINESS_DURATION;
    }

    public Duration reportDuration() {
        return REPORT_DURATION;
    }

    public Duration prepDuration() {
        return PREP_DURATION;
    }

    public Duration dayDuration() {
        return DAY_DURATION;
    }

    private int normalizeOffsetSeconds(Integer offsetSeconds) {
        return offsetSeconds == null ? 0 : Math.max(0, offsetSeconds);
    }
}
