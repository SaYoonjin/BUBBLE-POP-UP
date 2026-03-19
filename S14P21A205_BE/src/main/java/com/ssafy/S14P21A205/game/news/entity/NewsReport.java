package com.ssafy.S14P21A205.game.news.entity;

import com.ssafy.S14P21A205.game.season.entity.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "news_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_report_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private Integer day;

    @Column(name = "area_revenue_ranking", nullable = false, columnDefinition = "json")
    private String areaRevenueRanking;

    @Column(name = "area_population_ranking", nullable = false, columnDefinition = "json")
    private String areaPopulationRanking;

    @Column(name = "menu_entry_ranking", nullable = false, columnDefinition = "json")
    private String menuEntryRanking;

    @Column(name = "trend_keyword_ranking", nullable = false, columnDefinition = "json")
    private String trendKeywordRanking;

    @Column(name = "area_entry_ranking", nullable = false, columnDefinition = "json")
    private String areaEntryRanking;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private NewsReport(Season season, Integer day, String areaRevenueRanking, String areaPopulationRanking,
            String menuEntryRanking, String trendKeywordRanking, String areaEntryRanking) {
        this.season = season;
        this.day = day;
        this.areaRevenueRanking = areaRevenueRanking;
        this.areaPopulationRanking = areaPopulationRanking;
        this.menuEntryRanking = menuEntryRanking;
        this.trendKeywordRanking = trendKeywordRanking;
        this.areaEntryRanking = areaEntryRanking;
    }

    public static NewsReport create(Season season, int day, String areaRevenueRanking, String areaPopulationRanking,
            String menuEntryRanking, String trendKeywordRanking, String areaEntryRanking) {
        return new NewsReport(season, day, areaRevenueRanking, areaPopulationRanking,
                menuEntryRanking, trendKeywordRanking, areaEntryRanking);
    }

    public void updateRankings(String areaRevenueRanking, String menuEntryRanking, String areaEntryRanking) {
        this.areaRevenueRanking = areaRevenueRanking;
        this.menuEntryRanking = menuEntryRanking;
        this.areaEntryRanking = areaEntryRanking;
    }
}
