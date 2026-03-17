package com.ssafy.S14P21A205.game.day.resolver;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventScheduleResolver {

    private final DailyEventRepository dailyEventRepository;
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public List<GameDayStartResponse.EventSchedule> resolve(Long seasonId, int day) {
        List<DailyEvent> dailyEvents = dailyEventRepository.findBySeasonIdAndDayOrderByIdAsc(seasonId, day);
        List<GameDayStartResponse.EventSchedule> eventSchedule = new ArrayList<>();
        for (DailyEvent dailyEvent : dailyEvents) {
            RandomEvent event = dailyEvent.getEvent();
            Integer balanceChange = event.getCapitalFlat() == null || event.getCapitalFlat() == 0
                    ? null
                    : event.getCapitalFlat();
            eventSchedule.add(new GameDayStartResponse.EventSchedule(
                    seasonTimelineService.formatGameTime(dailyEvent.getApplyOffsetSeconds()),
                    event.getEventType(),
                    new GameDayStartResponse.Scope(null, null),
                    dailyEvent.getNewsTitle(),
                    normalizeScale(event.getPopulationRate()),
                    balanceChange
            ));
        }
        return eventSchedule;
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
