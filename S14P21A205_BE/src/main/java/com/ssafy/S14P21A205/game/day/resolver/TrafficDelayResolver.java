package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import com.ssafy.S14P21A205.game.environment.repository.TrafficDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.model.GameTimePoint;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import com.ssafy.S14P21A205.game.time.service.GameClockService;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrafficDelayResolver {

    private static final int[] DELIVERY_SECONDS_BY_TRAFFIC = {0, 5, 15, 20, 25, 35};

    private final TrafficDayRedisRepository trafficDayRedisRepository;
    private final TrafficRepository trafficRepository;

    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();
    private final GameClockService gameClockService = new GameClockService();

    public ResolvedTraffic resolve(
            Long locationId,
            int day,
            int totalDays,
            LocalDateTime currentDayStart,
            LocalDateTime effectiveNow
    ) {
        if (locationId == null || currentDayStart == null || day < 1 || totalDays < day) {
            return fallback(null);
        }

        Optional<LocalDate> targetDate = resolveTargetDate(locationId, day);
        if (targetDate.isEmpty()) {
            return fallback(null);
        }

        int gameHour = resolveGameHour(currentDayStart, day, totalDays, effectiveNow);
        LocalDateTime targetDateTime = targetDate.get().atTime(gameHour, 0);

        return trafficDayRedisRepository.findExact(locationId, targetDateTime)
                .map(entry -> new ResolvedTraffic(targetDateTime, entry.trafficStatus(), toDelaySeconds(entry.trafficStatus())))
                .or(() -> trafficRepository.findFirstByLocation_IdAndDate(locationId, targetDateTime)
                        .map(traffic -> new ResolvedTraffic(
                                targetDateTime,
                                traffic.getTrafficStatus(),
                                toDelaySeconds(traffic.getTrafficStatus())
                        )))
                .orElseGet(() -> fallback(targetDateTime));
    }

    private Optional<LocalDate> resolveTargetDate(Long locationId, int day) {
        List<Traffic> traffics = trafficRepository.findByLocationIdOrderByDateAsc(locationId);
        if (traffics.isEmpty()) {
            return Optional.empty();
        }

        LinkedHashSet<LocalDate> uniqueDates = new LinkedHashSet<>();
        for (Traffic traffic : traffics) {
            uniqueDates.add(traffic.getDate().toLocalDate());
        }

        List<LocalDate> orderedDates = new ArrayList<>(uniqueDates);
        if (day > orderedDates.size()) {
            return Optional.empty();
        }
        return Optional.of(orderedDates.get(day - 1));
    }

    private int resolveGameHour(
            LocalDateTime currentDayStart,
            int day,
            int totalDays,
            LocalDateTime effectiveNow
    ) {
        DayWindow dayWindow = seasonTimelineService.currentDay(currentDayStart, day, totalDays);
        GameTimePoint gameTimePoint = gameClockService.resolve(effectiveNow, dayWindow);
        long totalBusinessSeconds = seasonTimelineService.businessDuration().toSeconds();
        if (totalBusinessSeconds <= 0L) {
            return GameTimePolicy.BUSINESS_OPEN_HOUR;
        }

        int slotCount = GameTimePolicy.BUSINESS_CLOSE_HOUR - GameTimePolicy.BUSINESS_OPEN_HOUR;
        long boundedElapsed = Math.max(0L, Math.min(gameTimePoint.elapsedBusinessSeconds(), totalBusinessSeconds));
        int slotIndex = (int) Math.min(
                slotCount - 1L,
                (boundedElapsed * slotCount) / totalBusinessSeconds
        );
        return GameTimePolicy.BUSINESS_OPEN_HOUR + slotIndex;
    }

    private ResolvedTraffic fallback(LocalDateTime resolvedDateTime) {
        return new ResolvedTraffic(resolvedDateTime, TrafficStatus.NORMAL, toDelaySeconds(TrafficStatus.NORMAL));
    }

    private int toDelaySeconds(TrafficStatus trafficStatus) {
        int index = trafficStatus == null ? TrafficStatus.NORMAL.getValue() : trafficStatus.getValue();
        int clampedIndex = Math.max(1, Math.min(index, DELIVERY_SECONDS_BY_TRAFFIC.length - 1));
        return DELIVERY_SECONDS_BY_TRAFFIC[clampedIndex];
    }

    public record ResolvedTraffic(
            LocalDateTime resolvedDateTime,
            TrafficStatus trafficStatus,
            int delaySeconds
    ) {
    }
}
