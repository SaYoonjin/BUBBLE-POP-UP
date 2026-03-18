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

    @Operation(summary = "[테스트] 마감 뉴스 생성 (랭킹 갱신 + 마감 뉴스 1건)")
    @PostMapping("/test/closing/{seasonId}/{day}")
    public ResponseEntity<String> testClosingNews(@PathVariable Long seasonId, @PathVariable int day) {
        newsService.updateDayRankings(seasonId, day);
        return ResponseEntity.ok("마감 뉴스 생성 요청 완료 (비동기). seasonId=" + seasonId + ", day=" + day);
    }

    @Operation(summary = "[테스트] 영업 중 뉴스 생성 (메뉴 입점수 + 지역 입점수)")
    @PostMapping("/test/opening/{seasonId}/{day}")
    public ResponseEntity<String> testOpeningNews(@PathVariable Long seasonId, @PathVariable int day) {
        newsService.generateOpeningNews(seasonId, day);
        return ResponseEntity.ok("영업 중 뉴스 생성 요청 완료 (비동기). seasonId=" + seasonId + ", day=" + day);
    }
}
