package com.ssafy.S14P21A205.game.news.controller;

import com.ssafy.S14P21A205.game.news.dto.NewsListResponse;
import com.ssafy.S14P21A205.game.news.dto.NewsRankingResponse;
import com.ssafy.S14P21A205.game.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController implements NewsControllerDoc {

    private final NewsService newsService;

    @Override
    @GetMapping("/{day}")
    public ResponseEntity<NewsListResponse> getTodayNews(@PathVariable int day) {
        return ResponseEntity.ok(newsService.getTodayNews(day));
    }

    @Override
    @GetMapping("/{day}/ranking")
    public ResponseEntity<NewsRankingResponse> getAreaRankings(@PathVariable int day) {
        return ResponseEntity.ok(newsService.getAreaRankings(day));
    }
}
