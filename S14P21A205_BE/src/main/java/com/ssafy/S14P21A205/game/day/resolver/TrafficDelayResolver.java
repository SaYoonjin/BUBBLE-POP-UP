package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import com.ssafy.S14P21A205.game.environment.repository.TrafficDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
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

        int gameHour = resolveGameHour(currentDayStart, effectiveNow);
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

        long elapsedBusinessSeconds = java.time.Duration.between(businessStart, boundedNow).toSeconds();
        int slotCount = GameTimePolicy.BUSINESS_CLOSE_HOUR - GameTimePolicy.BUSINESS_OPEN_HOUR;
        int slotIndex = (int) Math.min(
                slotCount - 1L,
                (elapsedBusinessSeconds * slotCount) / totalBusinessSeconds
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