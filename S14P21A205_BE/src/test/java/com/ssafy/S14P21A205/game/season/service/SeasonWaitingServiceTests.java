package com.ssafy.S14P21A205.game.season.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.season.dto.GameWaitingResponse;
import com.ssafy.S14P21A205.game.season.dto.GameWaitingStatus;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SeasonWaitingServiceTests {

    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final SeasonWaitingService seasonWaitingService = new SeasonWaitingService(seasonRepository);

    @Test
    void getWaitingStatusReturnsInProgressWhenSeasonIsActive() {
        Season inProgressSeason = season(3L, SeasonStatus.IN_PROGRESS, 3, 7, LocalDateTime.of(2026, 3, 16, 10, 0));
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgressSeason));

        GameWaitingResponse response = seasonWaitingService.getWaitingStatus();

        assertEquals(GameWaitingStatus.IN_PROGRESS, response.status());
        assertNull(response.nextSeasonNumber());
        assertEquals(3, response.currentDay());
        assertNull(response.nextSeasonStartTime());
    }

    @Test
    void getWaitingStatusReturnsWaitingWhenNextSeasonIsScheduled() {
        ReflectionTestUtils.setField(
                seasonWaitingService,
                "clock",
                Clock.fixed(Instant.parse("2026-03-15T23:00:00Z"), ZoneId.of("Asia/Seoul"))
        );

        Season scheduledSeason = season(4L, SeasonStatus.SCHEDULED, null, 7, LocalDateTime.of(2026, 3, 16, 10, 0));
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED))
                .thenReturn(Optional.of(scheduledSeason));

        GameWaitingResponse response = seasonWaitingService.getWaitingStatus();

        assertEquals(GameWaitingStatus.WAITING, response.status());
        assertEquals(4, response.nextSeasonNumber());
        assertNull(response.currentDay());
        assertEquals(120, response.nextSeasonStartTime());
    }

    @Test
    void getWaitingStatusReturnsFallbackWaitingWhenNoSeasonExists() {
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED))
                .thenReturn(Optional.empty());
        when(seasonRepository.findFirstByOrderByIdDesc())
                .thenReturn(Optional.empty());

        GameWaitingResponse response = seasonWaitingService.getWaitingStatus();

        assertEquals(GameWaitingStatus.WAITING, response.status());
        assertEquals(1, response.nextSeasonNumber());
        assertNull(response.currentDay());
        assertNull(response.nextSeasonStartTime());
    }

    private Season season(
            Long id,
            SeasonStatus status,
            Integer currentDay,
            Integer totalDays,
            LocalDateTime startTime
    ) {
        Season season = mock(Season.class);
        when(season.getId()).thenReturn(id);
        when(season.getStatus()).thenReturn(status);
        when(season.getCurrentDay()).thenReturn(currentDay);
        when(season.getTotalDays()).thenReturn(totalDays);
        when(season.getStartTime()).thenReturn(startTime);
        return season;
    }
}
