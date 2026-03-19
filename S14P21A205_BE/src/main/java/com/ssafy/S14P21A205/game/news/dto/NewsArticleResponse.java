package com.ssafy.S14P21A205.game.news.dto;

import com.ssafy.S14P21A205.game.news.entity.NewsArticle;

public record NewsArticleResponse(
        Long newsId,
        String newsTitle,
        String newsContent
) {

    public static NewsArticleResponse from(NewsArticle article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getNewsTitle(),
                article.getNewsContent()
        );
    }
}
