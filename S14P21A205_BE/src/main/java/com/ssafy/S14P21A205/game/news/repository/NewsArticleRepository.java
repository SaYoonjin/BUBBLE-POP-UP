package com.ssafy.S14P21A205.game.news.repository;

import com.ssafy.S14P21A205.game.news.entity.NewsArticle;
import com.ssafy.S14P21A205.game.news.entity.NewsCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    List<NewsArticle> findByNewsReportIdOrderByIdAsc(Long newsReportId);

    List<NewsArticle> findByDayAndCategory(Integer day, NewsCategory category);

    List<NewsArticle> findByNewsReport_Season_IdAndDayOrderByIdAsc(Long seasonId, Integer day);

    boolean existsByNewsReportIdAndCategory(Long newsReportId, NewsCategory category);
}
