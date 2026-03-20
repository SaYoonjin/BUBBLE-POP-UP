package com.ssafy.S14P21A205.game.news.repository;

import com.ssafy.S14P21A205.game.news.entity.NewsReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsReportRepository extends JpaRepository<NewsReport, Long> {

    Optional<NewsReport> findBySeasonIdAndDay(Long seasonId, Integer day);

    Optional<NewsReport> findFirstBySeason_IdAndDay(Long seasonId, Integer day);

    List<NewsReport> findBySeasonIdOrderByDayAsc(Long seasonId);

    boolean existsBySeasonId(Long seasonId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NewsReport nr WHERE nr.season.id = :seasonId")
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
