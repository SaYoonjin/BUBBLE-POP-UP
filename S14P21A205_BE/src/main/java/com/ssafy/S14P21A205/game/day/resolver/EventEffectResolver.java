package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEffectResolver {

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");

    private final DailyEventRepository dailyEventRepository;
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public EventEffect resolve(
            Season season,
            int currentDay,
            LocalDateTime effectiveNow,
            Long locationId,
            Long menuId
    ) {
        List<DailyEvent> dailyEvents = dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(
                season.getId(),
                1,
                currentDay
        );

        long capitalChange = 0L;
        int stockChange = 0;
        BigDecimal populationEventMultiplier = DECIMAL_ONE;
        BigDecimal ingredientCostMultiplier = DECIMAL_ONE;
        List<GameStateResponse.AppliedEvent> appliedEvents = new ArrayList<>();

        for (DailyEvent dailyEvent : dailyEvents) {
            if (!matchesScope(dailyEvent, locationId, menuId)) {
                continue;
            }

            int appliedDay = resolveAppliedDay(dailyEvent);
            if (appliedDay < 1 || appliedDay > currentDay) {
                continue;
            }

            LocalDateTime appliedAt = seasonTimelineService.resolveAppliedAt(
                    season,
                    appliedDay,
                    dailyEvent.getApplyOffsetSeconds()
            );
            if (appliedAt.isAfter(effectiveNow)) {
                continue;
            }

            LocalDateTime endedAt = seasonTimelineService.resolveEndedAt(
                    season,
                    appliedDay,
                    dailyEvent.getExpireOffsetSeconds(),
                    dailyEvent.getEvent().getEndTime()
            );
            ResolvedEvent resolvedEvent = new ResolvedEvent(dailyEvent, appliedDay, appliedAt, endedAt);
            RandomEvent event = resolvedEvent.dailyEvent().getEvent();

            if (resolvedEvent.appliedDay() == currentDay) {
                capitalChange += event.getCapitalFlat() == null ? 0L : event.getCapitalFlat();
                stockChange += toWholeNumber(event.getStockFlat());
            }

            if (resolvedEvent.isActiveAt(effectiveNow)) {
                populationEventMultiplier = populationEventMultiplier.multiply(normalizeRate(event.getPopulationRate()));
                ingredientCostMultiplier = ingredientCostMultiplier.multiply(normalizeRate(event.getCostRate()));
                appliedEvents.add(new GameStateResponse.AppliedEvent(
                        event.getEventType(),
                        event.getEventType(),
                        event.getEventType(),
                        resolvedEvent.appliedAt()
                ));
            }
        }

        return new EventEffect(
                capitalChange,
                stockChange,
                populationEventMultiplier,
                ingredientCostMultiplier,
                appliedEvents
        );
    }

    private boolean matchesScope(DailyEvent dailyEvent, Long locationId, Long menuId) {
        Long targetLocationId = dailyEvent.getTargetLocationId();
        Long targetMenuId = dailyEvent.getTargetMenuId();
        if (targetLocationId != null && !targetLocationId.equals(locationId)) {
            return false;
        }
        return targetMenuId == null || targetMenuId.equals(menuId);
    }

    private int resolveAppliedDay(DailyEvent dailyEvent) {
        return dailyEvent.getEvent().getStartTime() == EventStartTime.NEXT_DAY
                ? dailyEvent.getDay() + 1
                : dailyEvent.getDay();
    }

    private int toWholeNumber(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return DECIMAL_ONE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record ResolvedEvent(
            DailyEvent dailyEvent,
            int appliedDay,
            LocalDateTime appliedAt,
            LocalDateTime endedAt
    ) {
        private boolean isActiveAt(LocalDateTime now) {
            return endedAt == null || now.isBefore(endedAt);
        }
    }

    public record EventEffect(
            long capitalChange,
            int stockChange,
            BigDecimal populationEventMultiplier,
            BigDecimal ingredientCostMultiplier,
            List<GameStateResponse.AppliedEvent> appliedEvents
    ) {
    }
}

