package com.ssafy.S14P21A205.game.day.service;

import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class SeasonTimeline {

    static final int BUSINESS_OPEN_HOUR = 10;
    static final int BUSINESS_CLOSE_HOUR = 22;
    static final int REALTIME_SEGMENT_COUNT = 3;

    private static final Duration PREP_DURATION = Duration.ofSeconds(50);
    private static final Duration BUSINESS_DURATION = Duration.ofMinutes(2);
    private static final Duration REPORT_DURATION = Duration.ofSeconds(10);
    private static final Duration DAY_DURATION = PREP_DURATION.plus(BUSINESS_DURATION).plus(REPORT_DURATION);
    private static final int GAME_BUSINESS_DURATION_MINUTES = (BUSINESS_CLOSE_HOUR - BUSINESS_OPEN_HOUR) * 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    DayTimeline currentDay(LocalDateTime currentDayStart, int currentDay, int totalDays) {
        return day(currentDayStart, currentDay, totalDays, currentDay);
    }

    DayTimeline day(LocalDateTime currentDayStart, int currentDay, int totalDays, int targetDay) {
        LocalDateTime targetDayStart = currentDayStart.minus(DAY_DURATION.multipliedBy((long) currentDay - targetDay));
        LocalDateTime businessStart = targetDayStart.plus(PREP_DURATION);
        LocalDateTime businessEnd = businessStart.plus(BUSINESS_DURATION);
        LocalDateTime reportEnd = businessEnd.plus(REPORT_DURATION);
        LocalDateTime dayOneStart = currentDayStart.minus(DAY_DURATION.multipliedBy(currentDay - 1L));
        LocalDateTime seasonPlayableEnd = dayOneStart.plus(DAY_DURATION.multipliedBy(totalDays));
        return new DayTimeline(targetDayStart, businessStart, businessEnd, reportEnd, seasonPlayableEnd);
    }

    LocalDateTime resolveAppliedAt(LocalDateTime currentDayStart, int currentDay, int totalDays, int appliedDay, Integer offsetSeconds) {
        DayTimeline appliedDayTimeline = day(currentDayStart, currentDay, totalDays, appliedDay);
        return appliedDayTimeline.businessStart().plusSeconds(normalizeOffsetSeconds(offsetSeconds));
    }

    LocalDateTime resolveEndedAt(
            LocalDateTime currentDayStart,
            int currentDay,
            int totalDays,
            int appliedDay,
            Integer expireOffsetSeconds,
            EventEndTime endTime
    ) {
        DayTimeline appliedDayTimeline = day(currentDayStart, currentDay, totalDays, appliedDay);
        if (expireOffsetSeconds != null && expireOffsetSeconds >= 0) {
            return appliedDayTimeline.businessStart().plusSeconds(expireOffsetSeconds);
        }
        return endTime == EventEndTime.SEASON_END
                ? appliedDayTimeline.seasonPlayableEnd()
                : appliedDayTimeline.businessEnd();
    }

    String formatGameTime(Integer offsetSeconds) {
        long boundedOffsetSeconds = Math.max(0L, Math.min(normalizeOffsetSeconds(offsetSeconds), BUSINESS_DURATION.toSeconds()));
        long gameMinutes = boundedOffsetSeconds * GAME_BUSINESS_DURATION_MINUTES / BUSINESS_DURATION.toSeconds();
        return LocalTime.of(BUSINESS_OPEN_HOUR, 0).plusMinutes(gameMinutes).format(TIME_FORMATTER);
    }

    Duration businessDuration() {
        return BUSINESS_DURATION;
    }

    Duration reportDuration() {
        return REPORT_DURATION;
    }

    Duration prepDuration() {
        return PREP_DURATION;
    }

    Duration dayDuration() {
        return DAY_DURATION;
    }

    private int normalizeOffsetSeconds(Integer offsetSeconds) {
        return offsetSeconds == null ? 0 : Math.max(0, offsetSeconds);
    }

    record DayTimeline(
            LocalDateTime dayStart,
            LocalDateTime businessStart,
            LocalDateTime businessEnd,
            LocalDateTime reportEnd,
            LocalDateTime seasonPlayableEnd
    ) {
    }
}
