package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import com.ssafy.S14P21A205.game.environment.repository.TrafficDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrafficDelayResolver {

    private static final int[] DELIVERY_SECONDS_BY_TRAFFIC = {0, 5, 15, 20, 25, 35};

    private final TrafficDayRedisRepository trafficDayRedisRepository;
    private final TrafficRepository trafficRepository;

    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public ResolvedTraffic resolve(
            Long seasonId,
            Long locationId,
            int day,
            int totalDays,
            LocalDateTime currentDayStart,
            LocalDateTime effectiveNow
    ) {
        if (seasonId == null || locationId == null || currentDayStart == null || day < 1 || totalDays < day) {
            return fallback(day, null);
        }

        int gameHour = resolveGameHour(currentDayStart, effectiveNow);
        return trafficDayRedisRepository.findHour(seasonId, locationId, day, gameHour)
                .map(entry -> new ResolvedTraffic(day, gameHour, entry.trafficStatus(), toDelaySeconds(entry.trafficStatus())))
                .or(() -> loadAndCacheDay(seasonId, locationId, day, currentDayStart).stream()
                        .filter(entry -> entry.hour() == gameHour)
                        .findFirst()
                        .map(entry -> new ResolvedTraffic(
                                day,
                                gameHour,
                                entry.trafficStatus(),
                                toDelaySeconds(entry.trafficStatus())
                        )))
                .orElseGet(() -> fallback(day, gameHour));
    }

    private int resolveGameHour(LocalDateTime currentDayStart, LocalDateTime effectiveNow) {
        LocalDateTime businessStart = currentDayStart.plus(seasonTimelineService.prepDuration());
        LocalDateTime businessEnd = businessStart.plus(seasonTimelineService.businessDuration());
        long totalBusinessSeconds = seasonTimelineService.businessDuration().toSeconds();
        if (totalBusinessSeconds <= 0L) {
            return GameTimePolicy.BUSINESS_OPEN_HOUR;
        }

        LocalDateTime boundedNow = effectiveNow;
        if (boundedNow.isBefore(businessStart)) {
            boundedNow = businessStart;
        }
        if (boundedNow.isAfter(businessEnd)) {
            boundedNow = businessEnd;
        }

        long elapsedBusinessSeconds = Duration.between(businessStart, boundedNow).toSeconds();
        int slotCount = GameTimePolicy.BUSINESS_CLOSE_HOUR - GameTimePolicy.BUSINESS_OPEN_HOUR;
        int slotIndex = (int) Math.min(
                slotCount - 1L,
                (elapsedBusinessSeconds * slotCount) / totalBusinessSeconds
        );
        return GameTimePolicy.BUSINESS_OPEN_HOUR + slotIndex;
    }

    private List<TrafficDayRedisRepository.TrafficEntry> loadAndCacheDay(
            Long seasonId,
            Long locationId,
            int day,
            LocalDateTime currentDayStart
    ) {
        LocalDateTime dayStart = currentDayStart.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
        List<Traffic> dayEntries = trafficRepository.findByLocation_IdAndDateBetweenOrderByDateAsc(locationId, dayStart, dayEnd);
        if (dayEntries.isEmpty()) {
            return List.of();
        }

        List<TrafficDayRedisRepository.TrafficEntry> cachedEntries = dayEntries.stream()
                .map(entry -> new TrafficDayRedisRepository.TrafficEntry(entry.getDate().getHour(), entry.getTrafficStatus()))
                .toList();
        trafficDayRedisRepository.saveDay(seasonId, locationId, day, cachedEntries);
        return cachedEntries;
    }

    private ResolvedTraffic fallback(Integer resolvedDay, Integer resolvedHour) {
        return new ResolvedTraffic(resolvedDay, resolvedHour, TrafficStatus.NORMAL, toDelaySeconds(TrafficStatus.NORMAL));
    }

    private int toDelaySeconds(TrafficStatus trafficStatus) {
        int index = trafficStatus == null ? TrafficStatus.NORMAL.getValue() : trafficStatus.getValue();
        int clampedIndex = Math.max(1, Math.min(index, DELIVERY_SECONDS_BY_TRAFFIC.length - 1));
        return DELIVERY_SECONDS_BY_TRAFFIC[clampedIndex];
    }

    public record ResolvedTraffic(
            Integer resolvedDay,
            Integer resolvedHour,
            TrafficStatus trafficStatus,
            int delaySeconds
    ) {
    }
}
