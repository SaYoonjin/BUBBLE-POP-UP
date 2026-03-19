package com.ssafy.S14P21A205.game.news.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "news_article")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_report_id", nullable = false)
    private NewsReport newsReport;

    @Column(name = "news_title", nullable = false, length = 200)
    private String newsTitle;

    @Column(name = "news_content", nullable = false, columnDefinition = "TEXT")
    private String newsContent;

    @Column(nullable = false)
    private Integer day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NewsCategory category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private NewsArticle(NewsReport newsReport, Integer day, NewsCategory category, String newsTitle,
            String newsContent) {
        this.newsReport = newsReport;
        this.day = day;
        this.category = category;
        this.newsTitle = newsTitle;
        this.newsContent = newsContent;
    }

    public static NewsArticle create(NewsReport newsReport, int day, NewsCategory category, String title,
            String content) {
        return new NewsArticle(newsReport, day, category, title, content);
    }
}
