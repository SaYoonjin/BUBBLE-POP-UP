package com.ssafy.S14P21A205.game.event.repository;

import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyEventRepository extends JpaRepository<DailyEvent, Long> {

    boolean existsBySeasonId(Long seasonId);

    void deleteBySeasonId(Long seasonId);

    @EntityGraph(attributePaths = {"event"})
    List<DailyEvent> findBySeasonIdAndDayOrderByIdAsc(Long seasonId, Integer day);

    @EntityGraph(attributePaths = {"event"})
    List<DailyEvent> findBySeasonIdAndDayBetweenOrderByDayAscIdAsc(Long seasonId, Integer startDay, Integer endDay);
}
