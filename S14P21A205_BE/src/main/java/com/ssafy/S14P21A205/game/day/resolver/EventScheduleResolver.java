package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventScheduleResolver {

    private final DailyEventRepository dailyEventRepository;
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public List<GameDayStartResponse.EventSchedule> resolve(Long seasonId, int day, Long locationId, Long menuId) {
        int startDay = Math.max(1, day - 1);
        List<DailyEvent> dailyEvents = dailyEventRepository.findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(seasonId, startDay, day);
        List<ResolvedSchedule> resolvedSchedules = new ArrayList<>();
        for (DailyEvent dailyEvent : dailyEvents) {
            if (!matchesScope(dailyEvent, locationId, menuId)) {
                continue;
            }
            if (resolveAppliedDay(dailyEvent) != day) {
                continue;
            }
            RandomEvent event = dailyEvent.getEvent();
            Integer balanceChange = event.getCapitalFlat() == null || event.getCapitalFlat() == 0
                    ? null
                    : event.getCapitalFlat();
            resolvedSchedules.add(new ResolvedSchedule(
                    dailyEvent.getApplyOffsetSeconds() == null ? 0 : dailyEvent.getApplyOffsetSeconds(),
                    new GameDayStartResponse.EventSchedule(
                    seasonTimelineService.formatGameTime(dailyEvent.getApplyOffsetSeconds()),
                    event.getEventName(),
                    resolveScope(dailyEvent),
                    event.getEventName(),
                    normalizeScale(event.getPopulationRate()),
                    balanceChange
                    )
            ));
        }
        return resolvedSchedules.stream()
                .sorted(Comparator.comparingInt(ResolvedSchedule::offsetSeconds))
                .map(ResolvedSchedule::eventSchedule)
                .toList();
    }

    private boolean matchesScope(DailyEvent dailyEvent, Long locationId, Long menuId) {
        if (dailyEvent.getTargetLocationId() != null && !dailyEvent.getTargetLocationId().equals(locationId)) {
            return false;
        }
        return dailyEvent.getTargetMenuId() == null || dailyEvent.getTargetMenuId().equals(menuId);
    }

    private GameDayStartResponse.Scope resolveScope(DailyEvent dailyEvent) {
        if (dailyEvent.getTargetLocationId() == null && dailyEvent.getTargetMenuId() == null) {
            return null;
        }
        return new GameDayStartResponse.Scope(dailyEvent.getTargetLocationId(), dailyEvent.getTargetMenuId());
    }

    private int resolveAppliedDay(DailyEvent dailyEvent) {
        return dailyEvent.getEvent().getStartTime() == EventStartTime.NEXT_DAY
                ? dailyEvent.getDay() + 1
                : dailyEvent.getDay();
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record ResolvedSchedule(
            int offsetSeconds,
            GameDayStartResponse.EventSchedule eventSchedule
    ) {
    }
}
