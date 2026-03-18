package com.ssafy.S14P21A205.news.controller;

import com.ssafy.S14P21A205.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "시즌 뉴스 생성", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/generate/{seasonId}")
    public ResponseEntity<Void> generateNews(@PathVariable Long seasonId) {
        newsService.generateSeasonNews(seasonId);
        return ResponseEntity.ok().build();
    }
}
