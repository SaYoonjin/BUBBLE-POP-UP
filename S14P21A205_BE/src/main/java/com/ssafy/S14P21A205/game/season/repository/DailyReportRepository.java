package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    @EntityGraph(attributePaths = {"store", "store.season", "store.location", "store.menu"})
    Optional<DailyReport> findByStoreIdAndDay(Long storeId, Integer day);

    boolean existsByStoreIdAndDay(Long storeId, Integer day);

    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu", "store.season"})
    List<DailyReport> findByStore_Season_IdAndDayLessThanOrderByStore_IdAscDayAsc(Long seasonId, Integer day);

    @Query("""
            SELECT dr.locationName, SUM(dr.revenue)
            FROM DailyReport dr
            WHERE dr.store.season.id = :seasonId AND dr.day = :day
            GROUP BY dr.locationName
            ORDER BY SUM(dr.revenue) DESC
            """)
    List<Object[]> sumRevenueByLocationAndDay(@Param("seasonId") Long seasonId, @Param("day") int day);

    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu"})
    @Query("""
            SELECT dr
            FROM DailyReport dr
            WHERE dr.store.season.id = :seasonId AND dr.day = :day
            ORDER BY dr.revenue DESC
            """)
    List<DailyReport> findTopBySeasonIdAndDayOrderByRevenueDesc(
            @Param("seasonId") Long seasonId, @Param("day") int day);

    @Query("""
            SELECT dr.store.id, dr.store.storeName, dr.menuName, SUM(dr.salesCount)
            FROM DailyReport dr
            WHERE dr.store.season.id = :seasonId
            GROUP BY dr.store.id, dr.store.storeName, dr.menuName
            ORDER BY SUM(dr.salesCount) DESC
            """)
    List<Object[]> sumSalesCountBySeasonId(@Param("seasonId") Long seasonId);

    @Query("""
            SELECT dr.locationName, COUNT(DISTINCT dr.store.id)
            FROM DailyReport dr
            WHERE dr.store.season.id = :seasonId AND dr.day = :day
            GROUP BY dr.locationName
            ORDER BY COUNT(DISTINCT dr.store.id) DESC
            """)
    List<Object[]> countStoresByLocationAndDay(
            @Param("seasonId") Long seasonId, @Param("day") int day);
}
