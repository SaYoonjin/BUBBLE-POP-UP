package com.ssafy.S14P21A205.news.repository;

import com.ssafy.S14P21A205.news.entity.NewsReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsReportRepository extends JpaRepository<NewsReport, Long> {

    Optional<NewsReport> findBySeasonIdAndDay(Long seasonId, Integer day);

    List<NewsReport> findBySeasonIdOrderByDayAsc(Long seasonId);

    boolean existsBySeasonId(Long seasonId);
}
