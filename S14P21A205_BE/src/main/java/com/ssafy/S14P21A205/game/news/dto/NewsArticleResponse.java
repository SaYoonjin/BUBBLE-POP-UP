package com.ssafy.S14P21A205.game.news.dto;

import com.ssafy.S14P21A205.game.news.entity.NewsArticle;

public record NewsArticleResponse(
        Long id,
        Integer day,
        String category,
        String title,
        String content
) {

    public static NewsArticleResponse from(NewsArticle article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getDay(),
                article.getCategory().name(),
                article.getNewsTitle(),
                article.getNewsContent()
        );
    }
}
