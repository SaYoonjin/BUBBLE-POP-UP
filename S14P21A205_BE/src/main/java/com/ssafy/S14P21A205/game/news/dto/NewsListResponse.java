package com.ssafy.S14P21A205.game.news.dto;

import com.ssafy.S14P21A205.game.news.entity.NewsArticle;
import java.util.List;

public record NewsListResponse(List<NewsArticleResponse> news) {

    public static NewsListResponse of(List<NewsArticle> articles) {
        return new NewsListResponse(
                articles.stream().map(NewsArticleResponse::from).toList());
    }
}
