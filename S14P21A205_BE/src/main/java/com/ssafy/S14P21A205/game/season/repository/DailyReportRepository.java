package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    @EntityGraph(attributePaths = {"store", "store.season", "store.location", "store.menu"})
    Optional<DailyReport> findByStoreIdAndDay(Long storeId, Integer day);

    @EntityGraph(attributePaths = {"store", "store.season", "store.location", "store.menu"})
    Optional<DailyReport> findFirstByStore_IdOrderByDayDesc(Long storeId);

    boolean existsByStoreIdAndDay(Long storeId, Integer day);

    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu", "store.season"})
    List<DailyReport> findByStore_Season_IdAndDayLessThanOrderByStore_IdAscDayAsc(Long seasonId, Integer day);
}
